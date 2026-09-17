package com.gis.supermercados.core.backup

import android.content.Context
import android.net.Uri
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.security.CryptoManager
import com.gis.supermercados.R
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.domain.model.BackupInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copia de seguridad y restauracion LOCAL (sin nube, sin internet).
 *
 * Formato del archivo .gisbak: un ZIP con
 *  - manifest.json  (metadatos, version de esquema, metodo de cifrado)
 *  - database.db    (copia exacta de la base de datos, cifrada)
 *
 * Cifrado:
 *  - Sin contrasena: clave del Android Keystore (solo restaurable en el mismo
 *    dispositivo/instalacion). Ideal para el backup automatico.
 *  - Con contrasena: clave derivada por PBKDF2 (120.000 iteraciones) + AES-GCM.
 *    Portable a otro dispositivo.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    private val cryptoManager: CryptoManager,
) {

    private val tag = "BackupManager"

    fun backupsDir(): File = File(context.getExternalFilesDir(null) ?: context.filesDir,
        AppConstants.DIR_BACKUPS).apply { if (!exists()) mkdirs() }

    /** Crea una copia de seguridad y devuelve su metadata. */
    suspend fun createBackup(passphrase: String? = null): AppResult<BackupInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val started = System.currentTimeMillis()
                val dbFile = checkpointAndCopyDatabase()
                val encryptedDb = encryptPayload(dbFile.readBytes(), passphrase)
                    ?: throw IllegalStateException("No se pudo cifrar la copia")

                val records = countRecords()
                val manifest = JSONObject().apply {
                    put(KEY_FORMAT, FORMAT_NAME)
                    put(KEY_FORMAT_VERSION, FORMAT_VERSION)
                    put(KEY_APP_VERSION, AppConstants.APP_VERSION_LABEL)
                    put(KEY_DB_VERSION, GisDatabase.VERSION)
                    put(KEY_CREATED_AT, started)
                    put(KEY_ENCRYPTED, true)
                    put(KEY_PASSPHRASE_PROTECTED, passphrase != null)
                    put(KEY_RECORDS, records)
                    put(KEY_DEVICE, android.os.Build.MODEL)
                }

                val fileName = buildFileName(started)
                val destination = File(backupsDir(), fileName)
                ZipOutputStream(FileOutputStream(destination).buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
                    zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry(ENTRY_DATABASE))
                    zip.write(encryptedDb)
                    zip.closeEntry()
                }
                dbFile.delete()

                val info = BackupInfo(
                    fileName = fileName,
                    filePath = destination.absolutePath,
                    sizeBytes = destination.length(),
                    createdAt = started,
                    encrypted = true,
                    protectedWithPassphrase = passphrase != null,
                    databaseVersion = GisDatabase.VERSION,
                    appVersion = AppConstants.APP_VERSION_LABEL,
                    records = records
                )
                AppLogger.i(tag, "Backup creado: $fileName (${info.readableSize})")
                trimOldBackups()
                AppResult.Success(info)
            }.getOrElse { error ->
                AppLogger.e(tag, "Fallo al crear la copia de seguridad", error)
                AppResult.Failure(UiText.of(R.string.backup_error_create), error)
            }
        }

    /** Lista las copias disponibles en el almacenamiento privado de la app. */
    fun listBackups(): List<BackupInfo> = runCatching {
        backupsDir().listFiles { file -> file.isFile && file.extension == AppConstants.BACKUP_EXTENSION }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file -> readManifest(file)?.let { it.first to file } }
            ?.map { (manifest, file) ->
                BackupInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    sizeBytes = file.length(),
                    createdAt = manifest.optLong(KEY_CREATED_AT, file.lastModified()),
                    encrypted = manifest.optBoolean(KEY_ENCRYPTED, true),
                    protectedWithPassphrase = manifest.optBoolean(KEY_PASSPHRASE_PROTECTED, false),
                    databaseVersion = manifest.optInt(KEY_DB_VERSION, 0),
                    appVersion = manifest.optString(KEY_APP_VERSION, ""),
                    records = manifest.optInt(KEY_RECORDS, 0)
                )
            }
            .orEmpty()
    }.onFailure { AppLogger.e(tag, "No se pudo listar las copias", it) }.getOrDefault(emptyList())

    /**
     * Restaura una copia sobre la base de datos actual.
     * IMPORTANTE: despues de restaurar, la aplicacion DEBE reiniciarse para
     * reabrir la base de datos (ver [restartApp]).
     */
    suspend fun restoreBackup(source: File, passphrase: String?): AppResult<BackupInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!source.exists()) throw IllegalArgumentException("Archivo de copia no encontrado")
                val (manifest, encryptedDb) = readBackupContent(source)
                    ?: throw IllegalStateException("La copia esta danada o no es de esta aplicacion")

                val needsPassphrase = manifest.optBoolean(KEY_PASSPHRASE_PROTECTED, false)
                if (needsPassphrase && passphrase.isNullOrBlank()) {
                    return@runCatching AppResult.Failure(UiText.of(R.string.backup_error_passphrase_required))
                }

                val dbBytes = decryptPayload(encryptedDb, passphrase)
                    ?: return@runCatching AppResult.Failure(UiText.of(R.string.backup_error_decrypt))

                if (!isValidSqliteFile(dbBytes)) {
                    AppLogger.e(tag, "Contenido restaurado no es una base de datos SQLite valida")
                    return@runCatching AppResult.Failure(UiText.of(R.string.backup_error_invalid_file))
                }

                val dbVersion = manifest.optInt(KEY_DB_VERSION, GisDatabase.VERSION)
                if (dbVersion > GisDatabase.VERSION) {
                    return@runCatching AppResult.Failure(UiText.of(R.string.backup_error_newer_version))
                }

                // 1) Cerrar Room para liberar el archivo
                database.close()

                // 2) Sustituir los archivos de la base de datos
                val target = context.getDatabasePath(AppConstants.DATABASE_NAME)
                target.parentFile?.mkdirs()
                listOf(target, File("${target.absolutePath}-wal"), File("${target.absolutePath}-shm"))
                    .forEach { if (it.exists()) it.delete() }
                FileOutputStream(target).use { it.write(dbBytes) }

                AppLogger.i(tag, "Base de datos restaurada desde ${source.name} (${dbBytes.size} bytes)")
                AppResult.Success(
                    BackupInfo(
                        fileName = source.name,
                        filePath = source.absolutePath,
                        sizeBytes = source.length(),
                        createdAt = manifest.optLong(KEY_CREATED_AT, source.lastModified()),
                        encrypted = true,
                        protectedWithPassphrase = needsPassphrase,
                        databaseVersion = dbVersion,
                        appVersion = manifest.optString(KEY_APP_VERSION, ""),
                        records = manifest.optInt(KEY_RECORDS, 0)
                    )
                )
            }.getOrElse { error ->
                AppLogger.e(tag, "Fallo al restaurar", error)
                AppResult.Failure(UiText.of(R.string.backup_error_restore), error)
            }
        }

    /** Copia una copia de seguridad al destino elegido por el usuario (SAF). */
    suspend fun exportTo(uri: Uri, fileName: String): AppResult<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(backupsDir(), fileName)
            if (!source.exists()) throw IllegalArgumentException("Copia no encontrada")
            val written = context.contentResolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("No se pudo abrir el destino")
            AppLogger.i(tag, "Copia exportada a SAF: $fileName")
            AppResult.Success(source.length())
        }.getOrElse { error ->
            AppLogger.e(tag, "Fallo al exportar la copia", error)
            AppResult.Failure(UiText.of(R.string.backup_error_export), error)
        }
    }

    /** Importa un archivo .gisbak elegido por el usuario al almacenamiento privado. */
    suspend fun importFrom(uri: Uri): AppResult<BackupInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("No se pudo leer el archivo")
            val stamp = AppDateTime.formatForFile(System.currentTimeMillis())
            val destination = File(backupsDir(), "importada_$stamp.${AppConstants.BACKUP_EXTENSION}")
            destination.writeBytes(bytes)
            val info = listBackups().firstOrNull { it.fileName == destination.name }
                ?: BackupInfo(
                    fileName = destination.name,
                    filePath = destination.absolutePath,
                    sizeBytes = destination.length(),
                    createdAt = System.currentTimeMillis(),
                    encrypted = true,
                    protectedWithPassphrase = false,
                    databaseVersion = 0,
                    appVersion = ""
                )
            AppLogger.i(tag, "Copia importada: ${info.fileName}")
            AppResult.Success(info)
        }.getOrElse { error ->
            AppLogger.e(tag, "Fallo al importar la copia", error)
            AppResult.Failure(UiText.of(R.string.backup_error_import), error)
        }
    }

    suspend fun deleteBackup(fileName: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        val file = File(backupsDir(), fileName)
        val deleted = file.exists() && file.delete()
        if (deleted) AppLogger.i(tag, "Copia eliminada: $fileName")
        AppResult.Success(deleted)
    }

    /** Reinicia la aplicacion tras una restauracion. */
    fun restartApp() {
        runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (intent != null) context.startActivity(intent)
        }.onFailure { AppLogger.e(tag, "No se pudo reiniciar la app", it) }
        // Forzamos la muerte del proceso: la base de datos fue cerrada y sustituida.
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // ------------------------- Operaciones internas -------------------------

    private suspend fun countRecords(): Int = runCatching {
        database.storeDao().count() + database.productDao().count() +
            database.saleDao().countBetween(0L, Long.MAX_VALUE) +
            database.expenseDao().countBetween(0L, Long.MAX_VALUE) +
            database.customerDao().getActive().size
    }.getOrDefault(0)

    /** Vuelca el WAL al archivo principal y devuelve una copia temporal. */
    private suspend fun checkpointAndCopyDatabase(): File {
        return withContext(Dispatchers.IO) {
            runCatching {
                database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            }.onFailure { AppLogger.w(tag, "wal_checkpoint fallo (no bloqueante): ${it.message}") }

            val source = context.getDatabasePath(AppConstants.DATABASE_NAME)
            if (!source.exists()) throw IllegalStateException("La base de datos no existe")
            val temp = File(context.cacheDir, "backup_db_${System.currentTimeMillis()}.db")
            source.copyTo(temp, overwrite = true)
            temp
        }
    }

    private fun buildFileName(timestamp: Long): String =
        "GIS_backup_${AppDateTime.formatForFile(timestamp)}.${AppConstants.BACKUP_EXTENSION}"

    /** Lee el manifest y la base de datos cifrada desde el ZIP. */
    private fun readBackupContent(file: File): Pair<JSONObject, ByteArray>? = runCatching {
        var manifest: JSONObject? = null
        var dbBytes: ByteArray? = null
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    ENTRY_MANIFEST -> manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                    ENTRY_DATABASE -> dbBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (manifest != null && dbBytes != null) manifest!! to dbBytes!! else null
    }.onFailure { AppLogger.e(tag, "ZIP ilegico", it) }.getOrNull()

    private fun readManifest(file: File): Pair<JSONObject, ByteArray>? = readBackupContent(file)

    private fun encryptPayload(data: ByteArray, passphrase: String?): ByteArray? =
        if (passphrase.isNullOrBlank()) {
            cryptoManager.encrypt(data)
        } else {
            runCatching {
                val salt = ByteArray(SALT_BYTES).also { java.security.SecureRandom().nextBytes(it) }
                val iv = ByteArray(IV_BYTES).also { java.security.SecureRandom().nextBytes(it) }
                val key = deriveKey(passphrase, salt)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                salt + iv + cipher.doFinal(data)
            }.onFailure { AppLogger.e(tag, "Fallo al cifrar con contrasena", it) }.getOrNull()
        }

    private fun decryptPayload(payload: ByteArray, passphrase: String?): ByteArray? =
        if (passphrase.isNullOrBlank()) {
            cryptoManager.decrypt(payload)
        } else {
            runCatching {
                val salt = payload.copyOfRange(0, SALT_BYTES)
                val iv = payload.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
                val data = payload.copyOfRange(SALT_BYTES + IV_BYTES, payload.size)
                val key = deriveKey(passphrase, salt)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                cipher.doFinal(data)
            }.onFailure { AppLogger.w(tag, "Contrasena incorrecta o archivo corrupto") }.getOrNull()
        }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /** Comprueba la cabecera magica de SQLite ("SQLite format 3"). */
    private fun isValidSqliteFile(bytes: ByteArray): Boolean {
        if (bytes.size < SQLITE_HEADER.size) return false
        return bytes.copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER)
    }

    /** Conserva solo las [AppConstants.BACKUP_KEEP_COUNT] copias mas recientes. */
    private fun trimOldBackups() {
        runCatching {
            val files = backupsDir().listFiles { f ->
                f.isFile && f.extension == AppConstants.BACKUP_EXTENSION && !f.name.startsWith("importada_")
            }?.sortedByDescending { it.lastModified() } ?: return
            files.drop(AppConstants.BACKUP_KEEP_COUNT).forEach {
                it.delete()
                AppLogger.i(tag, "Copia antigua eliminada: ${it.name}")
            }
        }
    }

    private companion object {
        const val FORMAT_NAME = "GIS_SUPERMERCADOS_BACKUP"
        const val FORMAT_VERSION = 1
        const val ENTRY_MANIFEST = "manifest.json"
        const val ENTRY_DATABASE = "database.db"
        const val KEY_FORMAT = "format"
        const val KEY_FORMAT_VERSION = "formatVersion"
        const val KEY_APP_VERSION = "appVersion"
        const val KEY_DB_VERSION = "dbVersion"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_ENCRYPTED = "encrypted"
        const val KEY_PASSPHRASE_PROTECTED = "passphraseProtected"
        const val KEY_RECORDS = "records"
        const val KEY_DEVICE = "device"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PBKDF2_ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
        // Cabecera magica de un archivo SQLite: "SQLite format 3" + byte NUL (16 bytes)
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}

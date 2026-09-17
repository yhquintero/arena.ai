package com.gis.supermercados.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones de esquema.
 *
 * PROCEDIMIENTO PARA ANADIR CAMBIOS (importante para no perder datos):
 * 1. Sube `GisDatabase.VERSION` en un 1.
 * 2. Modifica las entidades.
 * 3. Crea aqui la migracion `MIGRATION_X_Y` con el SQL exacto del cambio.
 * 4. Anadela al array [ALL].
 * 5. Compila: Room exportara el nuevo esquema en app/schemas/[VERSION].json
 *    (haz commit de ese archivo: es la "foto" contra la que se validan las pruebas).
 *
 * NUNCA uses fallbackToDestructiveMigration() en produccion: borraria los datos
 * del negocio. Solo se permite la reconstruccion automatica en caso de DOWNGRADE
 * (instalar una version antigua de la app), que es un escenario de desarrollo.
 */
object Migrations {

    /**
     * Migraciones activas. La version 1 es el esquema inicial, por lo que no
     * hay ninguna migracion que ejecutar todavia. Cuando subas la version,
     * registra aqui la migracion correspondiente.
     */
    val ALL: Array<Migration> = emptyArray()

    /**
     * PLANTILLA lista para usar cuando el esquema pase de 1 a 2.
     *
     * Demuestra el patron correcto: anadir columnas con valores por defecto
     * seguros (NOT NULL DEFAULT) para no romper instalaciones existentes ni
     * perder datos. Para activarla: (1) copia este bloque a un `val MIGRATION_1_2`,
     * (2) sube `GisDatabase.VERSION` a 2, (3) anade los campos a la entidad,
     * (4) incluyela en [ALL].
     */
    @Suppress("unused")
    val TEMPLATE_MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE products ADD COLUMN supplier_email TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE products ADD COLUMN reorder_quantity INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_products_supplier_email ON products (supplier_email)")
        }
    }

    /**
     * Plantilla reutilizable para migraciones que anaden columnas.
     * Uso: `addColumn("expenses", "notes", "TEXT NOT NULL DEFAULT ''")`.
     */
    fun addColumn(table: String, column: String, typeAndDefault: String): String =
        "ALTER TABLE $table ADD COLUMN $column $typeAndDefault"
}

package com.gis.supermercados.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utilidad para compartir archivos privados de la app mediante [FileProvider].
 *
 * Los directorios compartibles estan declarados en `res/xml/file_paths.xml`.
 */
object FileSharing {

    /** Uri de contenido del archivo (nunca exponer `file://` a otras apps). */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Intent de comparticion con permisos de lectura temporales. */
    fun shareIntent(
        context: Context,
        file: File,
        fileName: String,
        mimeType: String,
        title: String,
    ): Intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Tamano legible del archivo (B / KB / MB). */
    fun readableSize(sizeBytes: Long): String = when {
        sizeBytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_048_576.0)
        sizeBytes >= 1_024L -> String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1_024.0)
        else -> "$sizeBytes B"
    }
}

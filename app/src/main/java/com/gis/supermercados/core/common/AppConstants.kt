package com.gis.supermercados.core.common

/**
 * Constantes globales de la aplicacion.
 * Centralizarlas evita "numeros magicos" repartidos por el codigo y facilita
 * ajustar el comportamiento del negocio desde un unico punto.
 */
object AppConstants {

    // ---- Identidad ----
    const val APP_NAME = "Gestión Integral de Supermercados"
    const val APP_SHORT_NAME = "GIS Supermercados"
    const val APP_VERSION_LABEL = "1.0.0"
    const val REPORT_FOOTER = "Documento generado por GIS Supermercados · uso interno"

    // ---- Persistencia ----
    const val DATABASE_NAME = "gis_supermercados.db"
    const val SECURE_PREFS_NAME = "gis_secure_prefs"
    const val PLAIN_PREFS_NAME = "gis_prefs"

    // ---- Directorios de exportacion (almacenamiento privado de la app) ----
    const val DIR_REPORTS = "Reportes"
    const val DIR_BACKUPS = "Backups"
    const val DIR_RECEIPTS = "Justificantes"
    const val DIR_LOGS = "Logs"
    const val BACKUP_EXTENSION = "gisbak"

    // ---- Negocio (valores por defecto, editables en Ajustes) ----
    const val DEFAULT_CURRENCY_SYMBOL = "$"
    const val DEFAULT_TAX_RATE = 0.16
    const val DEFAULT_LOW_STOCK = 10
    const val DEFAULT_LOYALTY_POINTS_PER_100 = 1
    const val MAX_DISCOUNT_PERCENT = 100

    // ---- Seguridad ----
    const val PBKDF2_ITERATIONS = 120_000
    const val PASSWORD_MIN_LENGTH = 8
    const val MAX_FAILED_LOGINS = 5
    const val LOCK_MINUTES = 5
    const val SESSION_IDLE_TIMEOUT_MINUTES = 60
    const val BACKUP_WORK_NAME = "gis_auto_backup"
    const val BACKUP_KEEP_COUNT = 5

    // ---- Log interno ----
    const val LOG_EXPORT_LIMIT = 2_000

    // ---- Exportacion ----
    const val TOP_PRODUCTS_LIMIT = 20
    const val PDF_PAGE_WIDTH = 595.28f   // A4 vertical en puntos (210 x 297 mm)
    const val PDF_PAGE_HEIGHT = 841.89f
    const val PDF_MARGIN = 40f

    // ---- Claves de ajustes (tabla app_settings) ----
    object Keys {
        const val BUSINESS_NAME = "business_name"
        const val BUSINESS_LEGAL_NAME = "business_legal_name"
        const val BUSINESS_TAX_ID = "business_tax_id"
        const val BUSINESS_ADDRESS = "business_address"
        const val BUSINESS_PHONE = "business_phone"
        const val CURRENCY_SYMBOL = "currency_symbol"
        const val DEFAULT_TAX_RATE = "default_tax_rate"
        const val LOW_STOCK_THRESHOLD = "low_stock_threshold"
        const val AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        const val AUTO_BACKUP_HOURS = "auto_backup_hours"
        const val BACKUP_ENCRYPTED = "backup_encrypted"
        const val THEME_MODE = "theme_mode"
        const val DYNAMIC_COLORS = "dynamic_colors"
        const val REQUIRE_AUTH_ON_START = "require_auth_on_start"
        const val BIOMETRIC_ENABLED = "biometric_enabled"
        const val TICKET_PREFIX = "ticket_prefix"
        const val CREDIT_NOTE_PREFIX = "credit_note_prefix"
        const val FIRST_RUN_COMPLETED = "first_run_completed"
        const val SAMPLE_DATA_LOADED = "sample_data_loaded"
    }
}

package com.gis.supermercados.core.navigation

/**
 * Rutas de navegacion centralizadas.
 * Usar constantes evita errores de escritura en los `navigate("...")` y hace que
 * cualquier cambio de ruta se refleje en un unico sitio.
 */
object Routes {

    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val SALES = "sales"
    const val SALE_DETAIL = "sales/{saleId}"
    const val INVENTORY = "inventory"
    const val MOVEMENTS = "movements"
    const val PRODUCTS = "products"
    const val PRODUCT_EDIT = "products/edit?productId={productId}&storeId={storeId}"
    const val STORES = "stores"
    const val EXPENSES = "expenses"
    const val CUSTOMERS = "customers"
    const val REPORTS = "reports"
    const val REPORT_PREVIEW = "reports/preview?start={start}&end={end}&type={type}&period={period}&storeId={storeId}&format={format}"
    const val USERS = "users"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val LOGS = "logs"
    const val ABOUT = "about"

    fun saleDetail(saleId: Long) = "sales/$saleId"

    fun productEdit(productId: Long = 0L, storeId: Long = 0L) =
        "products/edit?productId=$productId&storeId=$storeId"

    fun reportPreview(
        start: Long,
        end: Long,
        type: String,
        period: String,
        storeId: Long,
        format: String,
    ) = "reports/preview?start=$start&end=$end&type=$type&period=$period&storeId=$storeId&format=$format"
}

/** Destinos de la barra de navegacion principal (telefono: abajo, tablet: lateral). */
enum class TopLevelDestination(val route: String) {
    DASHBOARD(Routes.DASHBOARD),
    POS(Routes.POS),
    INVENTORY(Routes.INVENTORY),
    REPORTS(Routes.REPORTS),
    MORE(Routes.SETTINGS);
}

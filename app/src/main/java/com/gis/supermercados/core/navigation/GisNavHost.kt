package com.gis.supermercados.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gis.supermercados.BuildConfig
import com.gis.supermercados.R
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.ui.catalog.ProductEditScreen
import com.gis.supermercados.ui.catalog.ProductsScreen
import com.gis.supermercados.ui.customers.CustomersScreen
import com.gis.supermercados.ui.dashboard.DashboardScreen
import com.gis.supermercados.ui.expenses.ExpensesScreen
import com.gis.supermercados.ui.inventory.InventoryScreen
import com.gis.supermercados.ui.inventory.MovementsScreen
import com.gis.supermercados.ui.pos.PosScreen
import com.gis.supermercados.ui.reports.ReportPreviewScreen
import com.gis.supermercados.ui.reports.ReportsScreen
import com.gis.supermercados.ui.sales.SaleDetailScreen
import com.gis.supermercados.ui.sales.SalesScreen
import com.gis.supermercados.ui.settings.BackupScreen
import com.gis.supermercados.ui.settings.LogsScreen
import com.gis.supermercados.ui.settings.SettingsScreen
import com.gis.supermercados.ui.settings.UsersScreen
import com.gis.supermercados.ui.stores.StoresScreen

/**
 * Grafo de navegacion principal.
 *
 * Los destinos sensibles verifican el rol de la sesion: si el usuario no tiene permiso se muestra
 * un aviso en lugar de la pantalla (defensa en profundidad junto con las validaciones del dominio).
 */
@Composable
fun GisNavHost(
    navController: NavHostController,
    session: AuthSession,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToPos = { navController.navigateSingleTop(Routes.POS) },
                onNavigateToReports = { navController.navigateSingleTop(Routes.REPORTS) },
                onNavigateToExpenses = { navController.navigateSingleTop(Routes.EXPENSES) },
                onNavigateToInventory = { navController.navigateSingleTop(Routes.INVENTORY) },
                onNavigateToSales = { navController.navigateSingleTop(Routes.SALES) }
            )
        }

        composable(Routes.POS) {
            if (session.role.canProcessSales) {
                PosScreen(onNavigateToSales = { navController.navigateSingleTop(Routes.SALES) })
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.SALES) {
            SalesScreen(
                onOpenSale = { saleId -> navController.navigate(Routes.saleDetail(saleId)) },
                onBack = back
            )
        }

        composable(
            route = Routes.SALE_DETAIL,
            arguments = listOf(navArgument(ARG_SALE_ID) { type = NavType.LongType })
        ) {
            SaleDetailScreen(onBack = back)
        }

        composable(Routes.INVENTORY) {
            InventoryScreen(
                onNavigateToMovements = { navController.navigate(Routes.MOVEMENTS) },
                onBack = back
            )
        }

        composable(Routes.MOVEMENTS) {
            MovementsScreen(onBack = back)
        }

        composable(Routes.PRODUCTS) {
            ProductsScreen(
                onNewProduct = { navController.navigate(Routes.productEdit()) },
                onEditProduct = { productId -> navController.navigate(Routes.productEdit(productId)) },
                onBack = back
            )
        }

        composable(
            route = Routes.PRODUCT_EDIT,
            arguments = listOf(
                navArgument(ARG_PRODUCT_ID) { type = NavType.LongType; defaultValue = 0L },
                navArgument(ARG_STORE_ID) { type = NavType.LongType; defaultValue = 0L }
            )
        ) {
            if (session.role.canManageProducts) {
                ProductEditScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.STORES) {
            if (session.role.canManageStores) {
                StoresScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.EXPENSES) {
            if (session.role.canRegisterExpenses || session.role.canViewReports) {
                ExpensesScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.CUSTOMERS) {
            if (session.role.canManageCustomers) {
                CustomersScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.REPORTS) {
            if (session.role.canViewReports) {
                ReportsScreen(onBack = back, onPreview = { navController.navigate(REPORT_PREVIEW_ROUTE) })
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(
            route = Routes.REPORT_PREVIEW,
            arguments = listOf(
                navArgument(ARG_START) { type = NavType.LongType; defaultValue = 0L },
                navArgument(ARG_END) { type = NavType.LongType; defaultValue = 0L },
                navArgument(ARG_TYPE) { type = NavType.StringType; defaultValue = "" },
                navArgument(ARG_PERIOD) { type = NavType.StringType; defaultValue = "" },
                navArgument(ARG_STORE_ID) { type = NavType.LongType; defaultValue = 0L },
                navArgument(ARG_FORMAT) { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            if (session.role.canViewReports) {
                ReportPreviewScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.USERS) {
            if (session.role.canManageUsers) {
                UsersScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = back,
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                onOpenLogs = { navController.navigate(Routes.LOGS) },
                onOpenUsers = { navController.navigate(Routes.USERS) }
            )
        }

        composable(Routes.BACKUP) {
            if (session.role.canManageSettings) {
                BackupScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.LOGS) {
            if (session.role.canManageSettings) {
                LogsScreen(onBack = back)
            } else {
                NoPermission(onBack = back)
            }
        }

        composable(Routes.ABOUT) { AboutContent(settings = settings) }
    }
}

/** Navegacion sin apilar duplicados del mismo destino. */
private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

/** Aviso cuando el rol activo no tiene permiso para el destino solicitado. */
@Composable
private fun NoPermission(onBack: () -> Unit) {
    EmptyState(
        title = stringResource(R.string.permission_denied_title),
        message = stringResource(R.string.permission_denied_message),
        icon = Icons.Rounded.Lock,
        actionLabel = stringResource(R.string.common_back),
        onAction = onBack,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    )
}

/** Informacion de la aplicacion (version, proposito y aviso de funcionamiento sin conexion). */
@Composable
private fun AboutContent(settings: AppSettings) {
    EmptyState(
        title = stringResource(R.string.app_name),
        message = stringResource(
            R.string.about_message,
            BuildConfig.VERSION_NAME,
            settings.businessName
        ),
        icon = Icons.Rounded.Lock
    )
}

/** Ruta base de la previsualizacion (los parametros son opcionales). */
private const val REPORT_PREVIEW_ROUTE = "reports/preview"

private const val ARG_SALE_ID = "saleId"
private const val ARG_PRODUCT_ID = "productId"
private const val ARG_STORE_ID = "storeId"
private const val ARG_START = "start"
private const val ARG_END = "end"
private const val ARG_TYPE = "type"
private const val ARG_PERIOD = "period"
private const val ARG_FORMAT = "format"

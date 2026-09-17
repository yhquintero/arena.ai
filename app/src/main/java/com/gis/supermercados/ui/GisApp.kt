package com.gis.supermercados.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gis.supermercados.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gis.supermercados.core.designsystem.LoadingState
import com.gis.supermercados.core.navigation.GisNavHost
import com.gis.supermercados.core.navigation.Routes
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.core.designsystem.GisTheme
import com.gis.supermercados.ui.auth.LoginScreen
import com.gis.supermercados.ui.common.LocalSnackbar

/** Raiz de la interfaz: tema, puerta de autenticacion y andamio principal. */
@Composable
fun GisApp(viewModel: MainViewModel, windowSizeClass: WindowSizeClass) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val hasUsers by viewModel.hasUsers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    GisTheme(themeMode = settings.themeMode, dynamicColors = settings.dynamicColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                isLoading -> LoadingState()

                session == null -> LoginScreen(
                    isFirstRun = hasUsers == false,
                    businessName = settings.businessName.ifBlank { "Gestion Integral Supermercados" }
                )

                else -> MainScaffold(
                    session = session as AuthSession,
                    settings = settings,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}

/** Andamio principal con navegacion adaptable (abajo en telefono, lateral en tablet). */
@Composable
private fun MainScaffold(
    session: AuthSession,
    settings: AppSettings,
    windowSizeClass: WindowSizeClass,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val destinations = destinationsFor(session.role)
    val showBars = destinations.any { it.route == currentRoute }

    CompositionLocalProvider(LocalSnackbar provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBars && !useRail) {
                    NavigationBar {
                        destinations.forEach { destination ->
                            val selected = backStackEntry?.destination?.hierarchy
                                ?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTopLevel(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(Modifier.fillMaxSize().padding(innerPadding)) {
                if (showBars && useRail) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            val selected = backStackEntry?.destination?.hierarchy
                                ?.any { it.route == destination.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navController.navigateToTopLevel(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                GisNavHost(
                    navController = navController,
                    session = session,
                    settings = settings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Navegacion a un destino principal sin apilar copias (comportamiento estandar). */
private fun androidx.navigation.NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Destinos visibles segun el rol (los permisos tambien se aplican en cada pantalla). */
@Composable
private fun destinationsFor(role: Role): List<DestinationSpec> = listOf(
    DestinationSpec(Routes.DASHBOARD, stringResource(R.string.nav_dashboard), Icons.Rounded.Dashboard),
    DestinationSpec(Routes.POS, stringResource(R.string.nav_pos), Icons.Rounded.PointOfSale),
    DestinationSpec(Routes.INVENTORY, stringResource(R.string.nav_inventory), Icons.Rounded.Inventory2),
    DestinationSpec(Routes.REPORTS, stringResource(R.string.nav_reports), Icons.Rounded.Assessment)
        .takeIf { role.canViewReports },
    DestinationSpec(Routes.SETTINGS, stringResource(R.string.nav_more), Icons.Rounded.MoreHoriz)
).filterNotNull()

private data class DestinationSpec(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

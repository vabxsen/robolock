package com.robolock.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robolock.app.ui.AppControlsViewModel
import com.robolock.app.ui.FocusViewModel
import com.robolock.app.ui.HomeViewModel
import com.robolock.app.ui.PermissionsViewModel
import com.robolock.app.ui.SettingsViewModel
import com.robolock.app.ui.StatsViewModel
import com.robolock.app.ui.appcontrols.AppControlsScreen
import com.robolock.app.ui.components.BottomNavItem
import com.robolock.app.ui.components.RobolockBottomBar
import com.robolock.app.ui.focus.FocusModeScreen
import com.robolock.app.ui.home.HomeScreen
import com.robolock.app.ui.onboarding.OnboardingScreen
import com.robolock.app.ui.onboarding.PermissionsScreen
import com.robolock.app.ui.settings.AboutScreen
import com.robolock.app.ui.settings.DetectionInfoScreen
import com.robolock.app.ui.settings.DistractionsScreen
import com.robolock.app.ui.settings.SettingsScreen
import com.robolock.app.ui.splash.SplashScreen
import com.robolock.app.ui.stats.StatsScreen

private const val ONBOARDING_PAGES = 2

@Composable
fun RobolockNavHost(
    startDestination: String,
    versionName: String,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevel = TopLevelDestination.fromRoute(currentRoute)

    Column(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.weight(1f),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onGetStarted = { navController.navigate(Routes.ONBOARDING) })
            }

            composable(Routes.ONBOARDING) {
                val home: HomeViewModel = viewModel()
                val state by home.state.collectAsStateWithLifecycle()
                var page by remember { mutableIntStateOf(0) }

                OnboardingScreen(
                    page = page,
                    pageCount = ONBOARDING_PAGES,
                    apps = state.apps,
                    onNext = {
                        if (page < ONBOARDING_PAGES - 1) page++
                        else navController.navigate(Routes.PERMISSIONS)
                    },
                    onSkip = { navController.navigate(Routes.PERMISSIONS) },
                )
            }

            composable(Routes.PERMISSIONS) {
                val vm: PermissionsViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                // Permission state is re-read every time this screen is resumed, so returning
                // from the system settings shows what is actually granted.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

                PermissionsScreen(
                    state = state,
                    onRequest = { permission -> context.startActivity(vm.intentFor(permission)) },
                    onContinue = {
                        vm.completeOnboarding()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

                HomeScreen(
                    state = state,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAppControls = { navController.navigate(Routes.APP_CONTROLS) },
                    onFixPermissions = { navController.navigate(Routes.PERMISSIONS) },
                )
            }

            composable(Routes.STATS) {
                val vm: StatsViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                StatsScreen(state = state, onSelectPeriod = vm::setPeriod)
            }

            composable(Routes.FOCUS) {
                val vm: FocusViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                FocusModeScreen(
                    state = state,
                    onSelectTier = vm::selectTier,
                    onSelectSchedule = vm::selectSchedule,
                    onSave = vm::save,
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    versionName = versionName,
                    onOpenFocus = { navController.navigate(Routes.FOCUS) },
                    onOpenAppControls = { navController.navigate(Routes.APP_CONTROLS) },
                    onOpenDistractions = { navController.navigate(Routes.DISTRACTIONS) },
                    onOpenDetectionInfo = { navController.navigate(Routes.DETECTION_INFO) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onSetDailySummary = vm::setDailySummary,
                    onClearData = vm::clearAllData,
                )
            }

            composable(Routes.APP_CONTROLS) {
                val vm: AppControlsViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                AppControlsScreen(
                    state = state,
                    onSetEnabled = vm::setEnabled,
                    onSetSessionLimit = vm::setSessionLimit,
                    onSetDailyLimit = vm::setDailyLimit,
                    onSetLaunchFriction = vm::setLaunchFriction,
                    onOpenDistractions = { navController.navigate(Routes.DISTRACTIONS) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.DISTRACTIONS) {
                val vm: SettingsViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                DistractionsScreen(
                    state = state,
                    onToggle = vm::toggleSurface,
                    onSetInterceptLinks = vm::setInterceptLinks,
                    onOpenDetectionInfo = { navController.navigate(Routes.DETECTION_INFO) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.DETECTION_INFO) {
                DetectionInfoScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(versionName = versionName, onBack = { navController.popBackStack() })
            }
        }

        // The bar shows only on the four top-level destinations, so onboarding and detail screens
        // get the full height.
        if (topLevel != null) {
            RobolockBottomBar(
                items = TopLevelDestination.entries.map {
                    BottomNavItem(
                        route = it.route,
                        label = it.label,
                        icon = it.icon,
                        selectedIcon = it.selectedIcon,
                    )
                },
                selectedRoute = topLevel.route,
                onSelect = { item ->
                    navController.navigate(item.route) {
                        // Single copy of each tab, and state preserved when switching between them.
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

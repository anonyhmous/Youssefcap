package com.driveflow.driver.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driveflow.driver.R
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.locale.AppLocaleManager
import com.driveflow.driver.ui.screens.alerts.AlertsScreen
import com.driveflow.driver.ui.screens.alerts.AlertsViewModel
import com.driveflow.driver.ui.screens.areas.AreasScreen
import com.driveflow.driver.ui.screens.areas.AreasViewModel
import com.driveflow.driver.ui.screens.blocked.BlockedScreen
import com.driveflow.driver.ui.screens.calibration.CalibrationScreen
import com.driveflow.driver.ui.screens.calibration.CalibrationViewModel
import com.driveflow.driver.ui.screens.commitments.CommitmentsScreen
import com.driveflow.driver.ui.screens.consent.ConsentScreen
import com.driveflow.driver.ui.screens.contact.ContactScreen
import com.driveflow.driver.ui.screens.guide.InstallGuideScreen
import com.driveflow.driver.ui.screens.playground.OverlayPlaygroundScreen
import com.driveflow.driver.ui.screens.home.HomeScreen
import com.driveflow.driver.ui.screens.home.HomeViewModel
import com.driveflow.driver.ui.screens.homeway.HomeWayScreen
import com.driveflow.driver.ui.screens.homeway.HomeWayViewModel
import com.driveflow.driver.ui.screens.news.NewsScreen
import com.driveflow.driver.ui.screens.news.NewsViewModel
import com.driveflow.driver.ui.screens.report.ReportScreen
import com.driveflow.driver.ui.screens.report.ReportViewModel
import com.driveflow.driver.ui.screens.rules.RulesScreen
import com.driveflow.driver.ui.screens.rules.RulesViewModel
import com.driveflow.driver.ui.screens.settings.SettingsScreen
import com.driveflow.driver.ui.screens.settings.SettingsViewModel
import com.driveflow.driver.ui.screens.subscribe.SubscribeScreen
import com.driveflow.driver.ui.screens.subscribe.SubscribeViewModel
import com.driveflow.driver.ui.screens.welcome.WelcomeScreen
import com.driveflow.driver.ui.theme.BrandGold
import com.driveflow.driver.ui.theme.BrandMint
import com.driveflow.driver.ui.theme.NavyBackground
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.OutlineNavy
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class NavTabItem(
    val screen: Screen,
    val titleRes: Int,
    val icon: ImageVector
)

@Composable
fun DriveFlowNavHost(
    navController: NavHostController = rememberNavController()
) {
    val settingsStore = AppGraph.settingsStore
    val coroutineScope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val welcomeSeen = settingsStore.welcomeSeenFlow.first()
        val consentVersion = settingsStore.consentVersionFlow.first()
        val contactName = settingsStore.contactNameFlow.first()
        val contactPhone = settingsStore.contactPhoneFlow.first()

        startDestination = when {
            !welcomeSeen -> Screen.Welcome.route
            consentVersion < 1 -> Screen.Consent.route
            contactName.isNullOrBlank() || contactPhone.isNullOrBlank() -> Screen.Contact.route
            else -> Screen.Home.route
        }
    }

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(NavyBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BrandMint)
        }
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomTabs = listOf(
        NavTabItem(Screen.Home, R.string.nav_home, Icons.Default.Home),
        NavTabItem(Screen.Settings, R.string.nav_settings, Icons.Default.Settings),
        NavTabItem(Screen.Help, R.string.nav_help, Icons.Default.HelpOutline)
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Settings.route,
        Screen.Help.route
    )

    Scaffold(
        containerColor = NavyBackground,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = NavySurface,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                    )
                ) {
                    NavigationBar(
                        containerColor = NavySurface,
                        contentColor = TextWhite
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.titleRes),
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.titleRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavyBackground,
                                    selectedTextColor = BrandGold,
                                    indicatorColor = BrandGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("tab_${tab.screen.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Welcome Screen
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onFinish = {
                        coroutineScope.launch {
                            settingsStore.setWelcomeSeen(true)
                            navController.navigate(Screen.Consent.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        coroutineScope.launch {
                            settingsStore.setWelcomeSeen(true)
                            navController.navigate(Screen.Consent.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 2. Consent Screen
            composable(Screen.Consent.route) {
                ConsentScreen(
                    onAgree = {
                        coroutineScope.launch {
                            settingsStore.setConsentVersion(1)
                            navController.navigate(Screen.Contact.route) {
                                popUpTo(Screen.Consent.route) { inclusive = true }
                            }
                        }
                    },
                    onReadPolicy = {
                        navController.navigate(Screen.Commitments.route)
                    }
                )
            }

            // 3. Contact Screen
            composable(Screen.Contact.route) {
                val language by settingsStore.languageFlow.collectAsStateWithLifecycle(initialValue = "ar")
                ContactScreen(
                    isArabic = AppLocaleManager.isRtl(language),
                    onSubmit = { name, phone, govId ->
                        coroutineScope.launch {
                            settingsStore.setContactName(name)
                            settingsStore.setContactPhone(phone)
                            val policy = settingsStore.policyFlow.first()
                            settingsStore.setPolicy(policy.copy(governorate = govId))
                            settingsStore.setSetupDone(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Contact.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 4. Home Screen
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel()
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onToggleReader = { homeViewModel.toggleReader(it) },
                    onToggleHomeWay = { homeViewModel.toggleHomeWay(it) },
                    onToggleApp = { app, enabled -> homeViewModel.toggleApp(app, enabled) },
                    onNavigateRules = { navController.navigate(Screen.Rules.route) },
                    onNavigateAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateHomeWay = { navController.navigate(Screen.HomeWay.route) },
                    onNavigatePlan = { navController.navigate(Screen.Subscribe.route) },
                    onSimulateAccessibility = { homeViewModel.markStepAccessibilityGranted() },
                    onSimulateOverlay = { homeViewModel.markStepOverlayGranted() }
                )
            }

            // 5. Rules Screen
            composable(Screen.Rules.route) {
                val rulesViewModel: RulesViewModel = viewModel()
                val state by rulesViewModel.formState.collectAsStateWithLifecycle()
                RulesScreen(
                    state = state,
                    onUpdateForm = { rulesViewModel.updateForm(it) },
                    onAddBannedWord = { rulesViewModel.addBannedWord(it) },
                    onRemoveBannedWord = { rulesViewModel.removeBannedWord(it) },
                    onSave = {
                        rulesViewModel.savePolicy {
                            // saved
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 6. Areas Screen
            composable(Screen.Areas.route) {
                val areasViewModel: AreasViewModel = viewModel()
                val uiState by areasViewModel.uiState.collectAsStateWithLifecycle()
                AreasScreen(
                    uiState = uiState,
                    onAddArea = { area, isAvoided -> areasViewModel.addArea(area, isAvoided) },
                    onRemoveArea = { area, isAvoided -> areasViewModel.removeArea(area, isAvoided) },
                    onMoveArea = { area, fromAvoided -> areasViewModel.moveArea(area, fromAvoided) },
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. Alerts Screen
            composable(Screen.Alerts.route) {
                val alertsViewModel: AlertsViewModel = viewModel()
                val uiState by alertsViewModel.uiState.collectAsStateWithLifecycle()
                AlertsScreen(
                    uiState = uiState,
                    onToggleAlert = { alertsViewModel.setAlertEnabled(it) },
                    onSelectMode = { alertsViewModel.setAlertMode(it) },
                    onTestAlert = { alertsViewModel.playTestAlert() },
                    onBack = { navController.popBackStack() }
                )
            }

            // 8. HomeWay Screen
            composable(Screen.HomeWay.route) {
                val homeWayViewModel: HomeWayViewModel = viewModel()
                val state by homeWayViewModel.formState.collectAsStateWithLifecycle()
                HomeWayScreen(
                    state = state,
                    onUpdateForm = { homeWayViewModel.updateForm(it) },
                    onSave = { homeWayViewModel.saveCurrentState() },
                    onClear = { homeWayViewModel.clearLocation() },
                    onBack = { navController.popBackStack() }
                )
            }

            // 9. Settings Screen
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onSetMarkerSize = { settingsViewModel.setMarkerSize(it) },
                    onSetBarOpacity = { settingsViewModel.setBarOpacityPercent(it) },
                    onSetBarPositionAuto = { settingsViewModel.setBarPositionAuto(it) },
                    onSetAutoOffHours = { settingsViewModel.setAutoOffHours(it) },
                    onToggleLanguage = { settingsViewModel.toggleLanguage() },
                    onNavigateSubscribe = { navController.navigate(Screen.Subscribe.route) }
                )
            }

            // 10. Help Screen
            composable(Screen.Help.route) {
                val helpViewModel: com.driveflow.driver.ui.screens.help.HelpViewModel = viewModel()
                val uiState by helpViewModel.uiState.collectAsStateWithLifecycle()
                com.driveflow.driver.ui.screens.help.HelpScreen(
                    uiState = uiState,
                    onNavigateReport = { navController.navigate(Screen.Report.route) },
                    onNavigateReplayWelcome = { navController.navigate(Screen.Welcome.route) },
                    onNavigateInstallGuide = { navController.navigate(Screen.InstallGuide.route) },
                    onNavigateCommitments = { navController.navigate(Screen.Commitments.route) },
                    onNavigateCalibration = { navController.navigate(Screen.Calibration.route) },
                    onNavigatePlayground = { navController.navigate(Screen.Playground.route) }
                )
            }

            // 11. Report Screen
            composable(Screen.Report.route) {
                val reportViewModel: ReportViewModel = viewModel()
                val state by reportViewModel.formState.collectAsStateWithLifecycle()
                ReportScreen(
                    state = state,
                    onSetCategory = { reportViewModel.setCategory(it) },
                    onSetMessage = { reportViewModel.setMessage(it) },
                    onSetAttachedImage = { reportViewModel.setAttachedImage(it) },
                    onSubmit = { reportViewModel.submitReport() },
                    onBack = { navController.popBackStack() }
                )
            }

            // 12. Subscribe Screen
            composable(Screen.Subscribe.route) {
                val subscribeViewModel: SubscribeViewModel = viewModel()
                val uiState by subscribeViewModel.uiState.collectAsStateWithLifecycle()
                SubscribeScreen(
                    uiState = uiState,
                    onSetLicenseKey = { subscribeViewModel.setLicenseKey(it) },
                    onSetAffiliateCode = { subscribeViewModel.setAffiliateCode(it) },
                    onActivate = { subscribeViewModel.activateLicense() },
                    onBack = { navController.popBackStack() }
                )
            }

            // 13. News Screen
            composable(Screen.News.route) {
                val newsViewModel: NewsViewModel = viewModel()
                val uiState by newsViewModel.uiState.collectAsStateWithLifecycle()
                NewsScreen(
                    uiState = uiState,
                    onMarkAllRead = { newsViewModel.markAllRead() },
                    onBack = { navController.popBackStack() }
                )
            }

            // 14. Commitments Screen
            composable(Screen.Commitments.route) {
                CommitmentsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 15. Calibration Screen (Hidden)
            composable(Screen.Calibration.route) {
                CalibrationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 16. Blocked Screen
            composable(Screen.Blocked.route) {
                BlockedScreen(
                    serverMessage = null,
                    onContactWhatsApp = {}
                )
            }

            // 17. Install Guide Screen
            composable(Screen.InstallGuide.route) {
                InstallGuideScreen(
                    onOpenAppSettings = {},
                    onBack = { navController.popBackStack() }
                )
            }

            // 18. Overlay Playground Screen
            composable(Screen.Playground.route) {
                OverlayPlaygroundScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

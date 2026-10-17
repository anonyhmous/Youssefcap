package com.driveflow.driver.ui.navigation

sealed class Screen(val route: String) {
    // Top Tabs
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Help : Screen("help")

    // Onboarding
    data object Welcome : Screen("welcome")
    data object Consent : Screen("consent")
    data object Contact : Screen("contact")

    // Features & Sub-screens
    data object Rules : Screen("rules")
    data object Areas : Screen("areas")
    data object Alerts : Screen("alerts")
    data object HomeWay : Screen("homeway")
    data object Subscribe : Screen("subscribe")
    data object News : Screen("news")
    data object Report : Screen("report")
    data object Commitments : Screen("commitments")
    data object Calibration : Screen("calibration")
    data object Blocked : Screen("blocked")
    data object InstallGuide : Screen("install_guide")
    data object Playground : Screen("playground")
}

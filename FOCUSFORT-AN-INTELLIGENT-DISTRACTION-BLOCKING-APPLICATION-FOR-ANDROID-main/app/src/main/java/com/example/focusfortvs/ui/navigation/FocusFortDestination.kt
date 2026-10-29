package com.example.focusfortvs.ui.navigation

sealed class FocusFortDestination(val route: String) {
    data object Splash : FocusFortDestination("splash")
    data object Welcome : FocusFortDestination("welcome")
    data object Privacy : FocusFortDestination("privacy_consent")
    data object Onboarding : FocusFortDestination("onboarding")
    data object PinSetup : FocusFortDestination("pin_setup")
    data object ModeSelect : FocusFortDestination("mode_select")
    data object AppPicker : FocusFortDestination("app_picker")
    data object Session : FocusFortDestination("session")
    data object Stats : FocusFortDestination("stats")
    data object Settings : FocusFortDestination("settings")
}

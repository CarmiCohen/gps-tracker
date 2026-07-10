package com.gps19.app

/**
 * Screen routes for Jetpack Compose Navigation.
 */
sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Tracker : Screen("tracker")
    object Viewer : Screen("viewer")
    object Diagnostics : Screen("diagnostics")
}

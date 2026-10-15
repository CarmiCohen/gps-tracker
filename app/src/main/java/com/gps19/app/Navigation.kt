package com.gps19.app

/**
 * Screen routes for Jetpack Compose Navigation.
 * Oct.14.17:
 * - A15 Hardening: Migrated NavigationState to MainUiState.kt to resolve 
 *   redeclaration errors. Maintained Screen sealed class for routing.
 */
sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Tracker : Screen("tracker")
    object Viewer : Screen("viewer")
    object Diagnostics : Screen("diagnostics")
}

package com.gps19.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * R799: Dynamic Theme based on Role (Tracker/Viewer).
 * v9.3.31:
 * - Performance Hardening (#092): Added remember { } block to ColorScheme 
 *   to prevent full-app recomposition loops and allocation storms.
 */

private fun getDarkColorScheme(appMode: String?) = darkColorScheme(
    primary = when (appMode) {
        "tracker" -> BrandJd
        "viewer" -> ViewerCyan
        else -> Slate500 
    },
    secondary = when (appMode) {
        "tracker" -> BrandJdDark
        "viewer" -> ViewerCyanDark
        else -> Slate800
    },
    tertiary = Amber500,
    background = Slate950,
    surface = Slate900,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Slate400,
    error = Rose500 
)

private fun getLightColorScheme(appMode: String?) = lightColorScheme(
    primary = when (appMode) {
        "tracker" -> BrandJdDark
        "viewer" -> ViewerCyanDark
        else -> Slate500
    },
    secondary = when (appMode) {
        "tracker" -> BrandJd
        "viewer" -> ViewerCyan
        else -> Slate400
    },
    tertiary = Amber500,
    background = Color.White,
    surface = Slate100,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Slate950,
    onSurface = Slate800,
    error = Rose500
)

@Composable
fun GpsTrackerTheme(
    appMode: String? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // R920: Stabilize color scheme to prevent allocation storms during telemetry updates.
    val colorScheme = remember(appMode, darkTheme) {
        if (darkTheme) getDarkColorScheme(appMode) else getLightColorScheme(appMode)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

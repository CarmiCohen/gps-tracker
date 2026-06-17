package com.gps19.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * R799: Dynamic Theme based on Role (Tracker/Viewer).
 * v5.851a:
 * - R851a: Changed Tracker role back to Green (Lime 500/600).
 * v5.681:
 * - Refactored TrackerOrange to ViewerOrange for semantic clarity.
 * v5.625:
 * - R815: Swapped Role Identity Colors.
 * - Tracker Role: Lime500 (restored from Teal)
 * - Viewer Role: ViewerOrange
 */

private fun getDarkColorScheme(appMode: String?) = darkColorScheme(
    primary = when (appMode) {
        "tracker" -> Lime500
        "viewer" -> ViewerOrange
        else -> Slate500 
    },
    secondary = when (appMode) {
        "tracker" -> Lime600
        "viewer" -> ViewerOrangeDark
        else -> Slate800
    },
    tertiary = Amber500,
    background = Slate950,
    surface = Slate900,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = White,
    onSurface = Slate400,
    error = Rose500 
)

private fun getLightColorScheme(appMode: String?) = lightColorScheme(
    primary = when (appMode) {
        "tracker" -> Lime600
        "viewer" -> ViewerOrangeDark
        else -> Slate500
    },
    secondary = when (appMode) {
        "tracker" -> Lime500
        "viewer" -> ViewerOrange
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
    val colorScheme = if (darkTheme) getDarkColorScheme(appMode) else getLightColorScheme(appMode)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

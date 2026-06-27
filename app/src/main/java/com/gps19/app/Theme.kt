package com.gps19.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * R799: Dynamic Theme based on Role (Tracker/Viewer).
 * v8.9.40:
 * - R865/R866 [Active]: Unified Identity Green (#367C2B) enforced.
 * v5.851a:
 * - R851a [Superseded]: Changed Tracker role back to Green (Replaced by R865).
 * v5.681:
 * - Refactored TrackerOrange to ViewerOrange for semantic clarity.
 * v5.625:
 * - R815 [Superseded]: Swapped Role Identity Colors (Replaced by R851a).
 */

private fun getDarkColorScheme(appMode: String?) = darkColorScheme(
    primary = when (appMode) {
        "tracker" -> BrandJd
        "viewer" -> ViewerOrange
        else -> Slate500 
    },
    secondary = when (appMode) {
        "tracker" -> BrandJdDark
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
        "tracker" -> BrandJdDark
        "viewer" -> ViewerOrangeDark
        else -> Slate500
    },
    secondary = when (appMode) {
        "tracker" -> BrandJd
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

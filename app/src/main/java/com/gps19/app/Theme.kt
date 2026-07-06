package com.gps19.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * R799: Dynamic Theme based on Role (Tracker/Viewer).
 * v9.1.0:
 * - R799e [Active]: JD Vivid Green (#78BE20) enforced as primary branding.
 * v9.0.4:
 * - R799d: Changed Viewer color to Cyan (#06B6D4) from Orange.
 * v8.9.40:
 * - R865/R866 [Active]: Unified Identity Green (#367C2B) enforced.
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
    onBackground = White,
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
    val colorScheme = if (darkTheme) getDarkColorScheme(appMode) else getLightColorScheme(appMode)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

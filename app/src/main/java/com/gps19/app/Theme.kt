package com.gps19.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * R799: Dynamic Theme based on Role (Tracker/Viewer).
 * Oct.15.21:
 * - SIMP-OCT15.20-S1: Animation Controller Composition. Introduced 
 *   LocalAnimationGating, gatedAnimateFloatAsState, and gatedInfinitePulse 
 *   to centralize Mali GPU anomaly handling across the UI layer.
 * v9.3.31:
 * - Performance Hardening (#092): Added remember { } block to ColorScheme 
 *   to prevent full-app recomposition loops and allocation storms.
 */

val LocalAnimationGating = staticCompositionLocalOf { false }

/**
 * gatedAnimateFloatAsState: Specialized wrapper that automatically snaps 
 * to target values when LocalAnimationGating is active (Mali Hardening).
 */
@Composable
fun gatedAnimateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = tween(),
    label: String = "FloatAnimation"
): State<Float> {
    val isThrottled = LocalAnimationGating.current
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = if (isThrottled) snap() else animationSpec,
        label = label
    )
}

/**
 * gatedInfinitePulse: Specialized wrapper for infinite transitions that 
 * freezes to targetValue when LocalAnimationGating is active.
 */
@Composable
fun gatedInfinitePulse(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "InfinitePulse"
): State<Float> {
    val isThrottled = LocalAnimationGating.current
    return if (isThrottled) {
        remember { mutableStateOf(targetValue) }
    } else {
        val transition = rememberInfiniteTransition(label = label)
        transition.animateFloat(initialValue, targetValue, animationSpec, label = label)
    }
}

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
    isMaliAnomaly: Boolean = false,
    content: @Composable () -> Unit
) {
    // R920: Stabilize color scheme to prevent allocation storms during telemetry updates.
    val colorScheme = remember(appMode, darkTheme) {
        if (darkTheme) getDarkColorScheme(appMode) else getLightColorScheme(appMode)
    }

    CompositionLocalProvider(LocalAnimationGating provides isMaliAnomaly) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

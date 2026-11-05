package com.jd.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val MAX_INSTRUMENT_FONT_SCALE = 1.3f

/**
 * Limits system font scaling for fixed-size instrument UI, where larger text would push
 * readings such as speed off the screen instead of wrapping.
 */
@Composable
fun CappedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(MAX_INSTRUMENT_FONT_SCALE)),
        content = content
    )
}

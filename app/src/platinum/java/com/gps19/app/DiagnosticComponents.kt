package com.gps19.app

import androidx.compose.runtime.Composable

/**
 * R-ID 610: Fundamental Suppression.
 * In the Platinum source set, this component is a NO-OP.
 * This physically removes the diagnostic UI from the composition tree
 * at the compilation level.
 */
@Composable
fun DiagnosticOverlay() {
    // No-op for Production/Platinum builds.
}

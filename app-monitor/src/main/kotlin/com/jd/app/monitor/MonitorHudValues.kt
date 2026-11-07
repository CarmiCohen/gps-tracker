package com.jd.app.monitor

import com.jd.core.ui.FixReading

/** One position fix from the Monitor device: where it is, and the [reading] its HUD line shows. */
data class LocalFix(
    val position: MapPosition,
    val reading: FixReading
)

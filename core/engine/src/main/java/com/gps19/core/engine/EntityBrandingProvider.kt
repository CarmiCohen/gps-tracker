package com.gps19.core.engine

/**
 * EntityBrandingProvider: Centralizes 'Tracker' vs 'Viewer' naming logic.
 * Fulfills SIMP-OCT14.13-S2.
 */
object EntityBrandingProvider {
    /**
     * Returns a title that is stripped of the current mode's prefix.
     * e.g. If isTracker=true, "Tracker: Offline" becomes "Offline".
     */
    fun getModeAwareTitle(isTracker: Boolean, title: String): String {
        return if (isTracker) {
            title.removePrefix("Tracker:").trim()
        } else {
            title.removePrefix("Viewer:").trim()
        }
    }
}

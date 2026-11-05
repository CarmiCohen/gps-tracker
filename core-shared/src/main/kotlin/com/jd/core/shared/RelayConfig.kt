package com.jd.core.shared

/** Single source of truth for the default relay address used by both apps. */
object RelayConfig {
    const val DEFAULT_URL = "https://gps-survival-relay.onrender.com"

    /** Returns the user's saved relay URL, or the default when none is saved. */
    fun resolve(savedUrl: String?): String = savedUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_URL
}

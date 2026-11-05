package com.jd.core.shared

import java.net.URI
import java.net.URISyntaxException

/** Single source of truth for the default relay address used by both apps. */
object RelayConfig {
    const val DEFAULT_URL = "https://gps-survival-relay.onrender.com"

    private val ALLOWED_SCHEMES = setOf("http", "https", "ws", "wss")

    /** True when the socket client can open this address. */
    fun isValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = URI(url.trim())
            uri.scheme?.lowercase() in ALLOWED_SCHEMES && !uri.host.isNullOrBlank()
        } catch (e: URISyntaxException) {
            false
        }
    }

    /** Returns the user's saved relay URL, or the default when none usable is saved. */
    fun resolve(savedUrl: String?): String = savedUrl?.trim()?.takeIf { isValid(it) } ?: DEFAULT_URL
}

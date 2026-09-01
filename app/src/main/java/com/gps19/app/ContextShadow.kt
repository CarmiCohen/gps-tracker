package com.gps19.app

import android.content.Context
import android.content.ContextWrapper

/**
 * ContextShadow: A specialized ContextWrapper that overrides package name lookups
 * to use the centralized ShadowCache. This prevents redundant IPC log spam 
 * during high-frequency system service interactions (Issue #894 / R759).
 */
class ContextShadow(base: Context) : ContextWrapper(base) {
    override fun getPackageName(): String {
        val cached = GpsApplication.PACKAGE_NAME
        return if (cached.isNotEmpty()) cached else super.getPackageName()
    }

    // Note: getOpPackageName is used by AppOps and certain system services.
    // Shadowing it here ensures attribution lookups are also optimized.
    override fun getOpPackageName(): String {
        val cached = GpsApplication.PACKAGE_NAME
        return if (cached.isNotEmpty()) cached else super.getOpPackageName()
    }
    
    override fun getApplicationContext(): Context {
        return this
    }
}

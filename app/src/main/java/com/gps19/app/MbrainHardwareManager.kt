package com.gps19.app

import timber.log.Timber

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * Issue #543: Integration of libmbrainSDK for MediaTek/Samsung chipset stay-alive hardening.
 */
object MbrainHardwareManager {

    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("mbrainSDK")
            isLibraryLoaded = true
            Timber.i("libmbrainSDK loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.e("libmbrainSDK load failed: ${e.message}")
        }
    }

    /**
     * Initializes the Mbrain engine with vendor-specific parameters.
     * @return 0 on success, negative error code otherwise.
     */
    external fun initMbrain(deviceId: String, flags: Int): Int

    /**
     * Triggers a hardware-level "poke" to prevent aggressive CPU idling.
     */
    external fun punchHardware(): Int

    /**
     * Sets the power budget for the radio/GNSS stack.
     */
    external fun setPowerBudget(budgetLevel: Int): Int

    fun isAvailable(): Boolean = isLibraryLoaded
}

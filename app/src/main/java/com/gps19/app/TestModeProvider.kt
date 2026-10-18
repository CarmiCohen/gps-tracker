package com.gps19.app

import javax.inject.Inject
import javax.inject.Singleton

/**
 * TestModeProvider: Contract for determining if the app is running in a test environment.
 * Resolves SIMP-OCT17.11-S1.
 */
interface TestModeProvider {
    fun isTestMode(): Boolean
}

@Singleton
class ProductionTestModeProvider @Inject constructor() : TestModeProvider {
    override fun isTestMode(): Boolean = false
}

/**
 * Testable implementation for instrumentation/unit tests.
 */
class ManualTestModeProvider(private var enabled: Boolean = false) : TestModeProvider {
    override fun isTestMode(): Boolean = enabled
    fun setEnabled(value: Boolean) {
        enabled = value
    }
}

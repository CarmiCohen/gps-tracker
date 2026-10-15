package com.gps19.app

import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LifecycleHydrationManager:
 * Centralizes and staggers the app hydration sequence to prevent UI stalls.
 * Oct.14.17:
 * - A15 Hardening: Significantly increased initial delays to allow 
 *   low-end Samsung GPUs to stabilize before attempting UI draws.
 */
@Singleton
class LifecycleHydrationManager @Inject constructor() {

    private val _hydrationLevel = MutableStateFlow(0)
    val hydrationLevel: StateFlow<Int> = _hydrationLevel.asStateFlow()

    private var hydrationJob: Job? = null

    fun startHydration(scope: CoroutineScope, useStaggered: Boolean, onComplete: () -> Unit) {
        if (hydrationJob?.isActive == true) return
        
        hydrationJob = scope.launch(Dispatchers.Main.immediate) {
            Timber.d("Hydration: Starting sequence (A15 Stabilized)")
            
            // Phase 1: Core Surface - Wait longer for GPU buffer swap
            delay(1500) // Increased from 500
            _hydrationLevel.value = 1
            
            delay(1000) 
            _hydrationLevel.value = 2
            
            delay(1000)
            _hydrationLevel.value = 3
            
            onComplete()

            // Phase 2: Map & Data Engine
            yield() 
            
            var retryCount = 0
            while (!GpsApplication.isOsmReady.get() && retryCount < 50) {
                delay(500)
                retryCount++
            }

            // Levels 4-7: Map Elements (Very slow for A15 stability)
            _hydrationLevel.value = 4
            delay(1000)
            _hydrationLevel.value = 5
            delay(1000)
            _hydrationLevel.value = 6
            delay(500)
            _hydrationLevel.value = 7

            // Phase 3: Heavy Overlays
            delay(1000)
            _hydrationLevel.value = 8
            delay(500)
            _hydrationLevel.value = 9
            delay(500)
            _hydrationLevel.value = 10
            delay(500)
            _hydrationLevel.value = 11
            
            Timber.d("Hydration: Full sequence completed (Level 11)")
        }
    }

    fun reset() {
        hydrationJob?.cancel()
        hydrationJob = null
        _hydrationLevel.value = 0
        Timber.d("Hydration: Manager reset")
    }
}

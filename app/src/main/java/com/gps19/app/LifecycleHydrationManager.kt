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
 * Sep.23.50:
 * - Issue #1338: Hardened hydration lifecycle by integrating all phases 
 *   (including Map/Overlay triggers) into the primary managed job to prevent 
 *   coroutine leaks during role transitions.
 */
@Singleton
class LifecycleHydrationManager @Inject constructor() {

    private val _hydrationLevel = MutableStateFlow(0)
    val hydrationLevel: StateFlow<Int> = _hydrationLevel.asStateFlow()

    private var hydrationJob: Job? = null

    fun startHydration(scope: CoroutineScope, useStaggered: Boolean, onComplete: () -> Unit) {
        if (hydrationJob?.isActive == true) return
        
        hydrationJob = scope.launch(Dispatchers.Main.immediate) {
            Timber.d("Hydration: Starting sequence (staggered=$useStaggered)")
            
            // Phase 1: Core Surface
            delay(if (useStaggered) 500 else 100)
            _hydrationLevel.value = 1
            
            delay(if (useStaggered) 750 else 200)
            _hydrationLevel.value = 2
            
            delay(if (useStaggered) 1000 else 300)
            _hydrationLevel.value = 3
            
            onComplete()

            // Phase 2: Map & Data Engine (Triggered on Idle)
            // We use a suspendable check instead of a detached IdleHandler to keep it in scope
            yield() // Give UI a chance to breath
            
            var retryCount = 0
            while (!GpsApplication.isOsmReady.get() && retryCount < 50) {
                delay(200)
                retryCount++
            }

            // Levels 4-7: Map Elements
            val mapDelay = if (useStaggered) 600L else 50L
            
            _hydrationLevel.value = 4
            delay(mapDelay)
            _hydrationLevel.value = 5
            delay(mapDelay)
            _hydrationLevel.value = 6
            delay(mapDelay)
            _hydrationLevel.value = 7

            // Phase 3: Heavy Overlays
            val overlayDelay = if (useStaggered) 800L else 100L
            
            delay(overlayDelay)
            _hydrationLevel.value = 8
            delay(overlayDelay)
            _hydrationLevel.value = 9
            delay(overlayDelay)
            _hydrationLevel.value = 10
            delay(overlayDelay)
            _hydrationLevel.value = 11
            
            Timber.d("Hydration: Full sequence completed (Level 11)")
        }
    }

    fun reset() {
        hydrationJob?.cancel()
        hydrationJob = null
        _hydrationLevel.value = 0
        Timber.d("Hydration: Manager reset to Level 0")
    }
}

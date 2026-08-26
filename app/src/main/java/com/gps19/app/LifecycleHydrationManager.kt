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
 * LifecycleHydrationManager (Issue #318/323):
 * Centralizes and staggers the app hydration sequence to prevent Davey stalls
 * on budget hardware (SM-A155F).
 * Aug.26.05:
 * - Issue #323 Hardening: Added Level 4 (Idle Map Hydration) using IdleHandler 
 *   to ensure heavy OSM engine initialization only occurs when the main 
 *   thread is free (R323).
 */
@Singleton
class LifecycleHydrationManager @Inject constructor() {

    private val _hydrationLevel = MutableStateFlow(0)
    val hydrationLevel: StateFlow<Int> = _hydrationLevel.asStateFlow()

    private var hydrationJob: Job? = null

    fun startHydration(scope: CoroutineScope, isA15: Boolean, onComplete: () -> Unit) {
        if (hydrationJob?.isActive == true) return
        
        hydrationJob = scope.launch(Dispatchers.Main.immediate) {
            Timber.d("Hydration: Starting sequence (A15=$isA15)")
            
            // Level 1: Surface (Basic UI shell ready)
            delay(if (isA15) 400 else 200)
            _hydrationLevel.value = 1
            Timber.d("Hydration: Level 1 (Surface)")

            // Level 2: Core/Nav (Navigation and basic data)
            delay(if (isA15) 600 else 300)
            _hydrationLevel.value = 2
            Timber.d("Hydration: Level 2 (Core)")

            // Level 3: Full (Heavy observations started, UI functional)
            delay(if (isA15) 800 else 400)
            _hydrationLevel.value = 3
            Timber.d("Hydration: Level 3 (Full)")
            
            onComplete()

            // Level 4: Map Engine (Idle-based to prevent Davey stalls during startup)
            // We use IdleHandler to wait for the first frames to finish rendering.
            Looper.myQueue().addIdleHandler {
                _hydrationLevel.value = 4
                Timber.d("Hydration: Level 4 (Map - Idle Triggered)")
                false // One-shot
            }
        }
    }

    fun reset() {
        hydrationJob?.cancel()
        _hydrationLevel.value = 0
    }
}

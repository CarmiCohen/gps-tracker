package com.gps19.app

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LifecycleHydrationManager (Issue #318):
 * Centralizes and staggers the app hydration sequence to prevent Davey stalls
 * on budget hardware (SM-A155F).
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
            
            // Level 1: Surface (Basic UI ready)
            delay(if (isA15) 500 else 300)
            _hydrationLevel.value = 1
            Timber.d("Hydration: Level 1 (Surface)")

            // Level 2: Core/Nav (Navigation and basic data)
            delay(if (isA15) 800 else 500)
            _hydrationLevel.value = 2
            Timber.d("Hydration: Level 2 (Core)")

            // Level 3: Full (Heavy observations and background tasks)
            delay(if (isA15) 1200 else 500)
            _hydrationLevel.value = 3
            Timber.d("Hydration: Level 3 (Full)")
            
            onComplete()
        }
    }

    fun reset() {
        hydrationJob?.cancel()
        _hydrationLevel.value = 0
    }
}

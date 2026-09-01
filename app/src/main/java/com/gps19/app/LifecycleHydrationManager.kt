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
 * LifecycleHydrationManager (Issue #318/323/739/758/874/880/882/885):
 * Centralizes and staggers the app hydration sequence to prevent Davey stalls
 * on budget hardware (SM-A155F).
 * Sep.01.12:
 * - Issue #885 Remediation: Decomposed Level 8 into 4 distinct phases (8-11). 
 *   Each heavy overlay (Settings, Log, Ribbons, GNSS) is now assigned its own 
 *   hydration level to eliminate the 885ms JIT spike on A15 hardware (R2.1).
 * Sep.01.04:
 * - Issue #880 Remediation: Refined staggered hydration strategy. Increased 
 *   map hydration delays from 300ms to 600ms for A15 hardware to eliminate 
 *   the residual 751ms Davey stall (R880).
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
            delay(if (isA15) 500 else 200)
            _hydrationLevel.value = 1
            Timber.d("Hydration: Level 1 (Surface)")

            // Level 2: Core/Nav (Navigation and basic data)
            delay(if (isA15) 750 else 300)
            _hydrationLevel.value = 2
            Timber.d("Hydration: Level 2 (Core)")

            // Level 3: Full (Heavy observations started, UI functional)
            delay(if (isA15) 1000 else 400)
            _hydrationLevel.value = 3
            Timber.d("Hydration: Level 3 (Full)")
            
            onComplete()

            // Map Hydration Sequence (Levels 4-7)
            Looper.myQueue().addIdleHandler {
                scope.launch(Dispatchers.Main.immediate) {
                    var retryCount = 0
                    while (!GpsApplication.isOsmReady.get() && retryCount < 50) {
                        delay(100)
                        retryCount++
                    }
                    
                    if (!GpsApplication.isOsmReady.get()) {
                        Timber.w("Hydration: OSM not ready after timeout, forcing Level 4")
                    }

                    // Level 4: Map Engine Base
                    _hydrationLevel.value = 4
                    Timber.d("Hydration: Level 4 (Map Engine Base)")
                    
                    val mapDelay = if (isA15) 600L else 100L
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 5
                    Timber.d("Hydration: Level 5 (Map Trails)")
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 6
                    Timber.d("Hydration: Level 6 (Map Current Positions)")
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 7
                    Timber.d("Hydration: Level 7 (Map Violations)")

                    // Phase 3: Overlay Hydration (Levels 8-11)
                    // Issue #885: Further split to distribute JIT load of heavy composables.
                    val overlayDelay = if (isA15) 800L else 150L
                    
                    delay(overlayDelay)
                    _hydrationLevel.value = 8
                    Timber.d("Hydration: Level 8 (Settings Overlay Ready)")
                    
                    delay(overlayDelay)
                    _hydrationLevel.value = 9
                    Timber.d("Hydration: Level 9 (Log Overlay Ready)")
                    
                    delay(overlayDelay)
                    _hydrationLevel.value = 10
                    Timber.d("Hydration: Level 10 (Ribbons Overlay Ready)")
                    
                    delay(overlayDelay)
                    _hydrationLevel.value = 11
                    Timber.d("Hydration: Level 11 (GNSS Detail Ready - Fully Hydrated)")
                }
                false // One-shot
            }
        }
    }

    fun reset() {
        hydrationJob?.cancel()
        _hydrationLevel.value = 0
    }
}

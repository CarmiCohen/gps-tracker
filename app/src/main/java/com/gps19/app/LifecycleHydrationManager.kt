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
 * LifecycleHydrationManager (Issue #318/323/739/758/874/880):
 * Centralizes and staggers the app hydration sequence to prevent Davey stalls
 * on budget hardware (SM-A155F).
 * Sep.01.04:
 * - Issue #880 Remediation: Refined staggered hydration strategy. Increased 
 *   map hydration delays from 300ms to 600ms for A15 hardware to eliminate 
 *   the residual 751ms Davey stall (R880).
 * Aug.31.07:
 * - Issue #874 Remediation: Further segmented Map Hydration. Separated Level 6 
 *   (Positions) and Level 7 (Violations) to ensure each hydration step remains 
 *   under the 700ms Davey threshold (R874).
 * Aug.28.10:
 * - Issue #758 Optimization: Integrated GpsApplication.isOsmReady gate. Map 
 *   hydration (Levels 4-8) now waits for the IO-thread pre-warming of the 
 *   OSM engine to complete, preventing Main-thread blocking during 
 *   SqlTileWriter initialization (R758).
 * Aug.26.16:
 * - Issue #739 Remediation: Decomposed Map Hydration into distinct phases. 
 *   This spreads Map Engine, Trails, Markers, and Final Overlays 
 *   over multiple frames using IdleHandler and staggered delays to eliminate 
 *   the 1.4s main-thread stall on A15 hardware (R739).
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

            // Map Hydration Sequence (Levels 4-8)
            // Use IdleHandler to wait for initial rendering to finish.
            Looper.myQueue().addIdleHandler {
                scope.launch(Dispatchers.Main.immediate) {
                    // Issue #758: Wait for OSM IO-thread pre-warming to complete
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
                    
                    // Issue #880: Stagger subsequent map overlays with higher delays for A15
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
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 8
                    Timber.d("Hydration: Level 8 (Map Fully Hydrated)")
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

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
 * LifecycleHydrationManager (Issue #318/323/739/758):
 * Centralizes and staggers the app hydration sequence to prevent Davey stalls
 * on budget hardware (SM-A155F).
 * Aug.28.10:
 * - Issue #758 Optimization: Integrated GpsApplication.isOsmReady gate. Map 
 *   hydration (Levels 4-7) now waits for the IO-thread pre-warming of the 
 *   OSM engine to complete, preventing Main-thread blocking during 
 *   SqlTileWriter initialization (R758).
 * Aug.26.16:
 * - Issue #739 Remediation: Decomposed Map Hydration into 4 distinct phases 
 *   (Levels 4-7). This spreads Map Engine, Trails, Markers, and Final Overlays 
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

            // Map Hydration Sequence (Levels 4-7)
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
                    
                    // Stagger subsequent map overlays to avoid frame drops
                    val mapDelay = if (isA15) 300L else 100L
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 5
                    Timber.d("Hydration: Level 5 (Map Trails)")
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 6
                    Timber.d("Hydration: Level 6 (Map Markers & Circles)")
                    
                    delay(mapDelay)
                    _hydrationLevel.value = 7
                    Timber.d("Hydration: Level 7 (Map Fully Hydrated)")
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

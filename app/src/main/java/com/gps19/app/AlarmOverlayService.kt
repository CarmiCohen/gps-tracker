package com.gps19.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gps19.core.engine.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * AlarmOverlayService: Implements SYSTEM_ALERT_WINDOW to ensure alarm visibility 
 * even when the app is in background and device is unlocked.
 * Oct.6.1:
 * - AUDIT-1006-1: Refactored state preparation. Migrated flow creation out of 
 *   composition scope to service-level initialization.
 * - AUDIT-1006-2: Optimized transition latency. Seeded sessionStateFlow with 
 *   immediate emission to prevent initial black frames.
 */
@AndroidEntryPoint
class AlarmOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var alarmManager: AppAlarmManager
    @Inject lateinit var repository: MainRepository
    @Inject lateinit var sirenLockoutUseCase: SirenLockoutUseCase
    @Inject lateinit var systemStatusProvider: SystemStatusProvider

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var stateProvider: UiStateProvider? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        initializeStateProvider()

        alarmManager.activeAlarmsFlow
            .onEach { list ->
                if (list.none { !it.isResolved && !it.isSirenDisabled }) {
                    stopSelf()
                }
            }
            .launchIn(lifecycleScope)

        showOverlay()
    }

    private fun initializeStateProvider() {
        val hudHealthFlow = combine(
            alarmManager.activeAlarmsFlow,
            sirenLockoutUseCase.silencedUntilRt
        ) { alarms, _ ->
            HudHealthState(
                activeAlarms = alarms,
                isAlarmSilenced = sirenLockoutUseCase.isLockedOut()
            )
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, HudHealthState())

        val kinematicFlow = repository.trackerLocation.map { update ->
            KinematicState().apply {
                trackerHealth.isLocationPending = update.integrity.isLocationPending
                trackerHealth.locationPendingReason = update.integrity.locationPendingReason
            }
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, KinematicState())

        val sessionStateFlow = flow {
            // Seed immediate value for AUDIT-1006-2
            emit(SessionUiState(permissions = systemStatusProvider.getPermissionState()))
            while(true) {
                kotlinx.coroutines.delay(30000)
                emit(SessionUiState(permissions = systemStatusProvider.getPermissionState()))
            }
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, SessionUiState())

        stateProvider = SimpleUiStateProvider(
            session = sessionStateFlow,
            kinematic = kinematicFlow,
            hudHealthState = hudHealthFlow
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (composeView != null) return
        val provider = stateProvider ?: return

        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AlarmOverlayService)
            setViewTreeViewModelStoreOwner(this@AlarmOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AlarmOverlayService)
            
            setContent {
                GpsTrackerTheme(appMode = "viewer") {
                    AlarmOverlay(
                        stateProvider = provider,
                        onHardwarePermissionClick = {
                            val intent = Intent(this@AlarmOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                action = "com.gps19.app.ACTION_VIEW_DIAGNOSTICS"
                            }
                            startActivity(intent)
                            stopSelf()
                        },
                        onMute = {
                            val summary = alarmManager.getActiveAlarmSummary().ifBlank { "Muted" }
                            repository.sendCommand(UiCommand.StopSiren(summary))
                            alarmManager.notifySirenManualStop()
                        },
                        onClose = {
                            stopSelf()
                        },
                        onGoToMap = {
                            val intent = Intent(this@AlarmOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                action = MainActivity.ACTION_NAVIGATE_TO_MAP
                            }
                            startActivity(intent)
                            stopSelf()
                        }
                    )
                }
            }
        }
        
        composeView = view

        try {
            windowManager.addView(view, layoutParams)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        composeView?.let { 
            it.disposeComposition()
            try {
                windowManager.removeViewImmediate(it)
            } catch (e: Exception) {
                // View might already be detached
            }
        }
        composeView = null
        stateProvider = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

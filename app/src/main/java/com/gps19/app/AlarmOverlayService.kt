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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * AlarmOverlayService: Implements SYSTEM_ALERT_WINDOW to ensure alarm visibility 
 * even when the app is in background and device is unlocked.
 * Oct.1.7:
 * - Issue #1402-B: System-Wide Alarm Overlay. Renders AlarmOverlay via WindowManager.
 *   Integrated SystemStatusProvider for reactive permission and hardware policy badges.
 */
@AndroidEntryPoint
class AlarmOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var alarmManager: AppAlarmManager
    @Inject lateinit var repository: MainRepository
    @Inject lateinit var sirenLockoutUseCase: SirenLockoutUseCase
    @Inject lateinit var systemStatusProvider: SystemStatusProvider

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

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
        
        // Auto-dismiss when all special alarms are resolved
        alarmManager.activeAlarmsFlow
            .onEach { list ->
                if (list.none { !it.isResolved && !it.isSirenDisabled }) {
                    stopSelf()
                }
            }
            .launchIn(lifecycleScope)

        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (composeView != null) return

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

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AlarmOverlayService)
            setViewTreeViewModelStoreOwner(this@AlarmOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AlarmOverlayService)
            
            setContent {
                val alarms by alarmManager.activeAlarmsFlow.collectAsState()
                val silencedUntil by sirenLockoutUseCase.silencedUntilRt.collectAsState(initial = 0L)
                val locationUpdate by repository.trackerLocation.collectAsState()
                
                val perms by produceState(initialValue = PermissionState()) {
                    value = systemStatusProvider.getPermissionState()
                }

                val isMuted = remember(silencedUntil) {
                    sirenLockoutUseCase.isLockedOut()
                }

                AlarmOverlay(
                    alarms = alarms,
                    isMuted = isMuted,
                    isLocationPending = locationUpdate.integrity.isLocationPending,
                    backgroundStatus = perms.backgroundStatus,
                    hasBackgroundRestriction = perms.hasBackgroundRestriction,
                    onHardwarePermissionClick = {
                        // Forward to settings via MainActivity
                        val intent = Intent(this@AlarmOverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            action = "com.gps19.app.ACTION_FIX_PERMISSIONS"
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
                            action = ACTION_NAVIGATE_TO_MAP
                        }
                        startActivity(intent)
                        stopSelf()
                    }
                )
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        composeView?.let { 
            windowManager.removeView(it)
        }
        composeView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

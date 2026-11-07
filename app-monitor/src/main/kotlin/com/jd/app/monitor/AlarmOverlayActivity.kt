package com.jd.app.monitor

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.ui.RevolutionTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The alarm shown over the lock screen by the full-screen alarm notification. It shows the engine's pending
 * alarms and closes once none is left, so opening it without an alarm shows nothing.
 */
@AndroidEntryPoint
class AlarmOverlayActivity : ComponentActivity() {

    @Inject lateinit var alarmEngine: MonitorAlarmEngine
    @Inject lateinit var identityManager: ForensicIdentityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            RevolutionTheme {
                val book by alarmEngine.alarms.collectAsStateWithLifecycle()
                val trackerId by identityManager.targetId.collectAsStateWithLifecycle(initialValue = null)
                LaunchedEffect(book.pending.isEmpty()) {
                    if (book.pending.isEmpty()) finish()
                }
                AlarmAlertScreen(book.pending, trackerId, onAcknowledge = alarmEngine::acknowledge)
            }
        }
    }
}

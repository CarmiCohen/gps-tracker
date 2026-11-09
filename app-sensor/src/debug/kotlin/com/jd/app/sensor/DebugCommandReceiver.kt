package com.jd.app.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.DebugCommand
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.SystemLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug builds only (SIMP-NOV8.100-S1): sets the Sensor ID from adb through the same store the Settings screen writes
 * (the running service reconnects under the new ID), and answers with one `REV_DEBUG_CMD` line.
 * Usage is in `TESTS/TEST_GUIDELINES.md`.
 */
class DebugCommandReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun identityManager(): ForensicIdentityManager
        fun dataStore(): SensorDataStore
        fun serviceStatus(): SensorServiceStatus
    }

    override fun onReceive(context: Context, intent: Intent) {
        val command = DebugCommand.parse(intent.action, DebugCommand.SENSOR_ACTIONS, intent::getStringExtra)
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SystemLogger.i(DebugCommand.LOG_TAG, run(command, deps))
            } catch (e: Exception) {
                SystemLogger.e(DebugCommand.LOG_TAG, "FAILED $command: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun run(command: DebugCommand, deps: Deps): String {
        val identity = deps.identityManager()
        when (command) {
            is DebugCommand.SetSensorId -> identity.setSelfId(command.id)
            is DebugCommand.Rejected -> return "REJECTED ${command.reason}"
            else -> Unit
        }
        val status = deps.serviceStatus()
        val motion = status.motion.value
        // Microphone and motion states let a remote check confirm the Sensor listens and is armed (Nov9.0-LiftTilt).
        return "OK $command self=${identity.selfId.first()} relay=${RelayConfig.resolve(deps.dataStore().relayUrl.first())}" +
            " mic=${status.microphone.value} motionArmed=${motion.isArmed} barometer=${motion.hasBarometer}"
    }
}

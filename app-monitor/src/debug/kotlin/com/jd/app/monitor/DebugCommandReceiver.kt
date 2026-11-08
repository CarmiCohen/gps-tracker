package com.jd.app.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.DebugCommand
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
 * Debug builds only (SIMP-NOV8.100-S1): pairs, unpairs, and edits Home Points from adb through the same stores the UI writes,
 * and answers with one `REV_DEBUG_CMD` line. Usage is in `TESTS/TEST_GUIDELINES.md`.
 */
class DebugCommandReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun identityManager(): ForensicIdentityManager
        fun dataStore(): MonitorDataStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        val command = DebugCommand.parse(intent.action, DebugCommand.MONITOR_ACTIONS, intent::getStringExtra)
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
        val store = deps.dataStore()
        when (command) {
            is DebugCommand.Pair -> identity.setTargetId(command.id)
            DebugCommand.Unpair -> identity.clearTargetId()
            is DebugCommand.AddHomePoint -> store.editGeofence { it.withPoint(command.point) }
            DebugCommand.ClearHomePoints -> store.editGeofence { it.cleared() }
            is DebugCommand.Rejected -> return "REJECTED ${command.reason}"
            DebugCommand.State, is DebugCommand.SetSensorId -> Unit
        }
        val fence = store.geofence.first()
        return "OK $command self=${identity.selfId.first()} target=${identity.targetId.first()} " +
            "homePoints=${fence.points.size} radius=${fence.radiusMeters} revision=${fence.revision} " +
            "points=${fence.points.joinToString(";") { "${it.lat},${it.lng}" }}"
    }
}

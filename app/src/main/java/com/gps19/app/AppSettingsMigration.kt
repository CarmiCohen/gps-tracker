package com.gps19.app

import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView

/**
 * AppSettingsMigration: Migrates data from legacy SharedPreferences to DataStore.
 * v8.9.37:
 * - Issue #334: Uptime Consistency. Consolidated redundant session timing fields into uptimeMs. (Formerly #271, originally #1)
 */
fun AppSettingsMigration(context: Context) = SharedPreferencesMigration<AppSettings>(
    context = context,
    sharedPreferencesName = "app_prefs"
) { sharedPrefs, currentData ->
    if (currentData.migratedFromPreferences) {
        currentData
    } else {
        val builder = currentData.toBuilder()
        builder.setMigratedFromPreferences(true)

        sharedPrefs.getString("app_mode", null)?.let { builder.setAppMode(it) }
        sharedPrefs.getString("tracker_id", null)?.let { builder.setTrackerId(it) }
        sharedPrefs.getString("viewer_id", null)?.let { builder.setViewerId(it) }
        sharedPrefs.getString("relay_url", null)?.let { builder.setRelayUrl(it) }
        
        sharedPrefs.getLong("app_start_time", 0L).let { if (it > 0) builder.setAppStartTime(it) }
        sharedPrefs.getLong("last_alarm_ack_ts", 0L).let { if (it > 0) builder.setLastAlarmAckTs(it) }
        sharedPrefs.getLong("home_points_ts", 0L).let { if (it > 0) builder.setHomePointsTs(it) }
        
        sharedPrefs.getFloat("max_distance", 0f).let { if (it > 0f) builder.setMaxDistance(it) }
        sharedPrefs.getFloat("max_accuracy", 0f).let { if (it > 0f) builder.setMaxAccuracy(it) }
        
        sharedPrefs.getBoolean("is_manual_exit", false).let { if (it) builder.setIsManualExit(true) }

        sharedPrefs.getLong("total_connected", 0L).let { if (it > 0) builder.setTotalConnected(it) }
        sharedPrefs.getLong("total_drop", 0L).let { if (it > 0) builder.setTotalDrop(it) }
        sharedPrefs.getLong("max_drop", 0L).let { if (it > 0) builder.setMaxDrop(it) }
        sharedPrefs.getLong("max_drop_ts", 0L).let { if (it > 0) builder.setMaxDropTs(it) }
        sharedPrefs.getLong("uptime", 0L).let { if (it > 0) builder.setUptime(it) }
        sharedPrefs.getLong("last_conn_ts", 0L).let { if (it > 0) builder.setLastConnectionTs(it) }
        sharedPrefs.getLong("last_disc_ts", 0L).let { if (it > 0) builder.setLastDisconnectionTs(it) }
        sharedPrefs.getLong("last_gps_ts", 0L).let { if (it > 0) builder.setLastGpsTs(it) }
        sharedPrefs.getString("selected_siren", null)?.let { builder.setSelectedSiren(it) }

        sharedPrefs.getFloat("tracker_lux_baseline", 0f).let { if (it > 0f) builder.setTrackerLuxBaseline(it) }
        
        sharedPrefs.getBoolean("is_mic_type_started", false).let { if (it) builder.setIsMicTypeStarted(true) }
        sharedPrefs.getLong("last_sit_ts", 0L).let { if (it > 0) builder.setLastSitTs(it) }

        val alertBuilder = AlertSettingsProto.newBuilder()
        sharedPrefs.getBoolean("alert_local_internet", true).let { alertBuilder.setLocalInternet(it) }
        sharedPrefs.getBoolean("alert_server_connection", true).let { alertBuilder.setServerConnection(it) }
        sharedPrefs.getBoolean("alert_relay_connection", true).let { alertBuilder.setRelayConnection(it) }
        sharedPrefs.getBoolean("alert_jammer_detection", true).let { alertBuilder.setJammerDetection(it) }
        sharedPrefs.getBoolean("alert_signal_loss", true).let { alertBuilder.setSignalLoss(it) }
        sharedPrefs.getBoolean("alert_gps_stalling", true).let { alertBuilder.setGpsStalling(it) }
        sharedPrefs.getBoolean("alert_distance", true).let { alertBuilder.setDistance(it) }
        sharedPrefs.getBoolean("alert_power", true).let { alertBuilder.setPower(it) }
        sharedPrefs.getBoolean("alert_low_battery", true).let { alertBuilder.setLowBattery(it) }
        sharedPrefs.getBoolean("alert_long_time_gap", true).let { alertBuilder.setLongTimeGap(it) }
        sharedPrefs.getBoolean("alert_high_temperature", true).let { alertBuilder.setHighTemperature(it) }
        sharedPrefs.getBoolean("alert_override_silence", true).let { alertBuilder.setOverrideSilence(it) }
        sharedPrefs.getBoolean("alert_use_max_volume", true).let { alertBuilder.setUseMaxVolume(it) }
        sharedPrefs.getBoolean("alert_vibration", true).let { alertBuilder.setVibrationEnabled(it) }
        sharedPrefs.getFloat("alert_volume", 0.8f).let { alertBuilder.setAlarmVolume(it) }
        sharedPrefs.getBoolean("alert_use_custom_volume", false).let { alertBuilder.setUseCustomVolume(it) }
        sharedPrefs.getBoolean("alert_tilt", true).let { alertBuilder.setTiltAlert(it) }
        sharedPrefs.getBoolean("alert_acoustic", true).let { alertBuilder.setAcousticAlert(it) }
        sharedPrefs.getBoolean("alert_lift", true).let { alertBuilder.setLiftAlert(it) }
        sharedPrefs.getBoolean("alert_tamper", true).let { alertBuilder.setTamperAlert(it) }
        sharedPrefs.getBoolean("alert_chair", true).let { alertBuilder.setChairOccupied(it) }
        sharedPrefs.getBoolean("alert_global_mute", false).let { alertBuilder.setGlobalMute(it) }
        builder.setAlertSettings(alertBuilder.build())

        builder.build()
    }
}

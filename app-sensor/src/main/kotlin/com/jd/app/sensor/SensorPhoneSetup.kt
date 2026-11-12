package com.jd.app.sensor

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jd.core.ui.BrandGold
import com.jd.core.ui.Slate500
import com.jd.core.ui.Slate900
import com.jd.core.ui.StatusGreen

/**
 * The Sensor's one-time Phone Setup (ISSUE-NOV10.8-O81, user decision 2026-11-12): only what the Sensor needs, for this
 * phone maker and Android version, each item with its live status. The Monitor keeps its own page
 * (`core-ui` `PhoneSetupOverlay`): overlay, DND and full-screen intents are Monitor needs, the Sensor makes no sound.
 */
internal enum class SensorSetupItem(val optional: Boolean = false) {
    PRECISE_LOCATION,
    BACKGROUND_LOCATION,
    MICROPHONE,
    NOTIFICATIONS,
    BATTERY_UNRESTRICTED,
    KEEP_WHEN_UNUSED,
    SAMSUNG_NEVER_SLEEPING,
    NOTIFICATION_ACCESS(optional = true)
}

/** What this phone grants the Sensor now. */
internal data class SensorSetupStatus(
    val preciseLocation: Boolean,
    val backgroundLocation: Boolean,
    val microphone: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
    val keptWhenUnused: Boolean,
    val notificationAccess: Boolean
) {
    /** True or false, or null where Android does not let an app read the setting (Samsung's sleeping-apps lists). */
    fun isGranted(item: SensorSetupItem): Boolean? = when (item) {
        SensorSetupItem.PRECISE_LOCATION -> preciseLocation
        SensorSetupItem.BACKGROUND_LOCATION -> backgroundLocation
        SensorSetupItem.MICROPHONE -> microphone
        SensorSetupItem.NOTIFICATIONS -> notifications
        SensorSetupItem.BATTERY_UNRESTRICTED -> batteryUnrestricted
        SensorSetupItem.KEEP_WHEN_UNUSED -> keptWhenUnused
        SensorSetupItem.SAMSUNG_NEVER_SLEEPING -> null
        SensorSetupItem.NOTIFICATION_ACCESS -> notificationAccess
    }

    /** Items known to be missing, optional ones not counted: the number on the console's Phone Setup line. */
    fun missingCount(items: List<SensorSetupItem>): Int =
        items.count { !it.optional && isGranted(it) == false }
}

/** The items this phone shows: "all the time" exists from Android 10, the unused-app pause from Android 11. */
internal fun sensorSetupItems(sdkInt: Int = Build.VERSION.SDK_INT, manufacturer: String = Build.MANUFACTURER): List<SensorSetupItem> =
    SensorSetupItem.entries.filter { item ->
        when (item) {
            SensorSetupItem.BACKGROUND_LOCATION -> sdkInt >= Build.VERSION_CODES.Q
            SensorSetupItem.KEEP_WHEN_UNUSED -> sdkInt >= Build.VERSION_CODES.R
            SensorSetupItem.SAMSUNG_NEVER_SLEEPING -> manufacturer.equals("samsung", ignoreCase = true)
            else -> true
        }
    }

internal fun readSensorSetupStatus(context: Context): SensorSetupStatus {
    fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return SensorSetupStatus(
        preciseLocation = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        backgroundLocation = hasBackgroundLocation(context),
        microphone = granted(OPTIONAL_MICROPHONE_PERMISSION),
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName),
        keptWhenUnused = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || context.packageManager.isAutoRevokeWhitelisted,
        notificationAccess = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    )
}

/** The live status, read again each time the user comes back from a system settings page or permission dialog. */
@Composable
internal fun rememberSensorSetupStatus(): State<SensorSetupStatus> {
    val context = LocalContext.current
    val status = remember { mutableStateOf(readSensorSetupStatus(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status.value = readSensorSetupStatus(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return status
}

@Composable
internal fun SensorPhoneSetupOverlay(
    isServiceRunning: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val status by rememberSensorSetupStatus()
    val items = remember { sensorSetupItems() }
    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && isServiceRunning) startSensorService(context) // The service takes the microphone on its next start command.
    }
    val backgroundLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun open(intent: Intent) = context.startActivity(intent)
    fun appInfo() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    Surface(modifier = Modifier.fillMaxSize(), color = Slate900) {
        Column(
            modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sensor Phone Setup", color = BrandGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${Build.MANUFACTURER.uppercase()} ${Build.MODEL.uppercase()}, Android ${Build.VERSION.RELEASE}",
                        color = Slate500,
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))

            items.forEachIndexed { index, item ->
                val number = index + 1
                val granted = status.isGranted(item)
                when (item) {
                    SensorSetupItem.PRECISE_LOCATION -> SetupStatusItem(
                        "$number. Precise Location", granted,
                        "Tracking needs precise location. In App info > Permissions > Location, turn on 'Use precise location'.",
                        "Open App Info"
                    ) { open(appInfo()) }
                    SensorSetupItem.BACKGROUND_LOCATION -> SetupStatusItem(
                        "$number. Location 'Allow all the time'", granted,
                        "Lets a Sensor restarted after a reboot track at once, without a tap on its notification.",
                        "Allow All the Time"
                    ) { backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                    SensorSetupItem.MICROPHONE -> SetupStatusItem(
                        "$number. Microphone", granted,
                        "Used to detect noise. Without it the Sensor runs without noise detection.",
                        "Grant Microphone"
                    ) { microphoneLauncher.launch(OPTIONAL_MICROPHONE_PERMISSION) }
                    SensorSetupItem.NOTIFICATIONS -> SetupStatusItem(
                        "$number. Notifications", granted,
                        "Android shows the running Sensor as a notification; after a reboot, a tap on it lets the Sensor take location and the microphone.",
                        "Open Notification Settings"
                    ) { open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                    SensorSetupItem.BATTERY_UNRESTRICTED -> SetupStatusItem(
                        "$number. Battery 'Unrestricted'", granted,
                        "Keeps Android from putting the Sensor to sleep in the background.",
                        "Allow Unrestricted"
                    ) {
                        @android.annotation.SuppressLint("BatteryLife") // The Sensor must run all the time.
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                        open(intent)
                    }
                    SensorSetupItem.KEEP_WHEN_UNUSED -> SetupStatusItem(
                        "$number. Keep the Sensor When Unused", granted,
                        "The Sensor is rarely opened. In App info, turn off 'Pause app activity if unused' (on some phones 'Remove permissions if app is unused'), or Android stops it after a few months.",
                        "Open App Info"
                    ) { open(appInfo()) }
                    SensorSetupItem.SAMSUNG_NEVER_SLEEPING -> SetupStatusItem(
                        "$number. Samsung 'Never sleeping apps'", granted,
                        "Samsung puts apps to sleep on its own. In Battery > Background usage limits, add GPS Sensor to 'Never sleeping apps' and keep it out of 'Sleeping apps' and 'Deep sleeping apps'. Android does not let the app read this list: check it yourself.",
                        "Open Samsung Battery"
                    ) {
                        try {
                            open(Intent("com.samsung.android.sm.ACTION_BATTERY"))
                        } catch (_: ActivityNotFoundException) { // Not a Samsung battery page: the general list.
                            open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                    SensorSetupItem.NOTIFICATION_ACCESS -> SetupStatusItem(
                        "$number. Notification Access (optional)", granted,
                        "Lets the Sensor tell a silent notification's vibration from a knock at the highest sensitivity settings. Only the fact that a notification vibrates is used; its content is not read.",
                        "Grant Notification Access"
                    ) { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Close", color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

/** One item: its status, and its button only while the item is missing or cannot be read. */
@Composable
private fun SetupStatusItem(
    title: String,
    granted: Boolean?,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    val (statusText, statusColor, icon) = when (granted) {
        true -> Triple("Done", StatusGreen, Icons.Default.CheckCircle)
        false -> Triple("Missing", BrandGold, Icons.Default.Warning)
        null -> Triple("Check yourself", Slate500, Icons.Default.Info)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = description,
            color = Slate500,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 28.dp, top = 2.dp)
        )
        if (granted != true) {
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.padding(start = 28.dp).heightIn(min = 32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(buttonText, fontSize = 11.sp)
            }
        }
    }
}

/** The console's Phone Setup line. */
internal fun phoneSetupLineText(missing: Int): String =
    if (missing == 0) "PHONE SETUP: COMPLETE" else "PHONE SETUP: $missing MISSING"

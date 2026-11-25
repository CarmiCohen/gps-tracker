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
import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import com.jd.core.ui.BrandGold
import com.jd.core.ui.OverlayHeader
import com.jd.core.ui.Slate500
import com.jd.core.ui.Slate900
import com.jd.core.ui.StatusGreen

/**
 * The Sensor's one-time Phone Setup (ISSUE-NOV10.8-O81, user decision 2026-11-12): only what the Sensor needs
 * ([SensorSetupItem]), for this phone maker and Android version, each item with its live status. The Monitor keeps its
 * own page (`core-ui` `PhoneSetupOverlay`): overlay, DND and full-screen intents are Monitor needs, the Sensor makes no
 * sound. What is missing here is also reported to the Monitor ([sensorSetupReport], ISSUE-NOV10.8-O83 part 2).
 *
 * This class: what this phone grants the Sensor now.
 */
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

    /** The items of [items] known to be missing, optional ones not counted: what the Monitor is told. */
    fun missingItems(items: List<SensorSetupItem>): List<SensorSetupItem> =
        items.filter { !it.optional && isGranted(it) == false }

    /** The number on the console's Phone Setup line. */
    fun missingCount(items: List<SensorSetupItem>): Int = missingItems(items).size
}

/**
 * What the Sensor tells the Monitor about its phone (ISSUE-NOV10.8-O83 part 2): the [missing] setup items, and what
 * the running service lacks although the setup allows it. A microphone without permission is the missing Microphone
 * item, and one switched off on the Monitor is the Monitor user's own choice: neither is a fault here. A phone without
 * a light sensor is one whatever the switch: the Monitor shows it only while its light detection is on (ISSUE-NOV22.4-O130).
 */
internal fun sensorSetupReport(
    missing: List<SensorSetupItem>,
    microphone: MicrophoneState,
    refused: RefusedAccess,
    behaviourRevision: Long,
    hasLightSensor: Boolean = true
): SensorSetupReport = SensorSetupReport(
    missing = missing,
    faults = listOfNotNull(
        SensorFault.LOCATION_NEEDS_TAP.takeIf { refused.location },
        when (microphone) {
            MicrophoneState.NEEDS_APP_OPEN -> SensorFault.MICROPHONE_NEEDS_TAP
            MicrophoneState.SILENCED -> SensorFault.MICROPHONE_SILENCED
            MicrophoneState.UNAVAILABLE -> SensorFault.MICROPHONE_UNAVAILABLE
            MicrophoneState.OFF, MicrophoneState.NO_PERMISSION, MicrophoneState.LISTENING, MicrophoneState.TURNED_OFF -> null
        },
        SensorFault.NO_LIGHT_SENSOR.takeUnless { hasLightSensor }
    ),
    behaviourRevision = behaviourRevision
)

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
            OverlayHeader(
                title = "Sensor Phone Setup",
                color = BrandGold,
                onClose = onClose,
                fontSize = 22.sp,
                subtitle = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL.uppercase()}, Android ${Build.VERSION.RELEASE}"
            )
            Spacer(Modifier.height(16.dp))

            items.forEachIndexed { index, item ->
                val number = index + 1
                val granted = status.isGranted(item)
                when (item) {
                    SensorSetupItem.PRECISE_LOCATION -> SetupStatusItem(
                        "$number. ${item.label}", granted,
                        "Tracking needs precise location. In App info > Permissions > Location, turn on 'Use precise location'.",
                        "Open App Info"
                    ) { open(appInfo()) }
                    SensorSetupItem.BACKGROUND_LOCATION -> SetupStatusItem(
                        "$number. ${item.label}", granted,
                        "Lets a Sensor restarted after a reboot track at once, without a tap on its notification.",
                        "Allow All the Time"
                    ) { backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                    SensorSetupItem.MICROPHONE -> SetupStatusItem(
                        "$number. ${item.label}", granted,
                        "Used to detect noise. Without it the Sensor runs without noise detection.",
                        "Grant Microphone"
                    ) { microphoneLauncher.launch(OPTIONAL_MICROPHONE_PERMISSION) }
                    SensorSetupItem.NOTIFICATIONS -> SetupStatusItem(
                        "$number. ${item.label}", granted,
                        "Android shows the running Sensor as a notification; after a reboot, a tap on it lets the Sensor take location and the microphone.",
                        "Open Notification Settings"
                    ) { open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                    SensorSetupItem.BATTERY_UNRESTRICTED -> SetupStatusItem(
                        "$number. ${item.label}", granted,
                        "Keeps Android from putting the Sensor to sleep in the background.",
                        "Allow Unrestricted"
                    ) {
                        @android.annotation.SuppressLint("BatteryLife") // The Sensor must run all the time.
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                        open(intent)
                    }
                    SensorSetupItem.KEEP_WHEN_UNUSED -> SetupStatusItem(
                        "$number. ${item.label}", granted,
                        "The Sensor is rarely opened. In App info, turn off 'Pause app activity if unused' (on some phones 'Remove permissions if app is unused'), or Android stops it after a few months.",
                        "Open App Info"
                    ) { open(appInfo()) }
                    SensorSetupItem.SAMSUNG_NEVER_SLEEPING -> SetupStatusItem(
                        "$number. ${item.label}", granted,
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
                        "$number. ${item.label}", granted,
                        "Lets the Sensor tell a silent notification's vibration from a knock at the highest sensitivity settings. Only the fact that a notification vibrates is used; its content is not read.",
                        "Grant Notification Access"
                    ) { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                }
                Spacer(Modifier.height(12.dp))
            }

            // No Close button: Back closes the screen (ISSUE-NOV25.22-O151).
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

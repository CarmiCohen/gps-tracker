# Project Revolution: Hardening & Audit Plan

## 1. Startup Latency (Davey) Remediation
- **Lazy Map Inflation**: The Monitor app must not initialize the Map engine during `onCreate`. It must show a lightweight "Status/Splash" frame and defer Map rendering until the first frame is drawn.
- **Background Initialization**: Heavy I/O components (ForensicBuffer MMap, DataStore) must be initialized on a background thread using a `Deferred` pattern to protect the 200ms cold-start budget.

## 2. 100% Kotlin Sensing Precision
- **Nanosecond Delta Math**: All integral and velocity calculations in `SensingEngine.kt` must use the actual `event.timestamp` delta (nanoseconds) provided by the Android Sensor API to account for irregular sampling rates.
- **Zero-Allocation Hot Path**: The sensing loop must use primitive arrays and object pooling (Flyweight) to prevent Garbage Collection (GC) pauses during 100Hz operation.
- **Spoofing Guard**: Cross-verifies GPS velocity against IMU integrals. Alert triggers only if GPS is "Locked" with high SNR but static, while IMU detects movement > 2m/s.

## 3. Reliability & Foreground Compliance
- **Android 14+ FGS**: The Sensor's foreground service declares `location|remoteMessaging|dataSync`.
- **Direct Boot Aware**: Service and BootReceiver are marked `directBootAware="true"`. DataStore and ForensicBuffer are initialized in the `DeviceProtectedStorage` context.
- **Reincarnation**: `onTaskRemoved` in SensorService schedules a one-second restart alarm to survive user-swipes in Recents.

## 4. Operational Forensic Hardening
- **Buffered Alert Perception**: `ViolationEvent` includes an `isBuffered` flag. The Monitor UI renders forensic/replayed alerts in grayscale to distinguish from live telemetry.
- **Stealth Mode**: The Sensor's Admin HUD is passcode-locked by default.
- **Local Trail TTL**: Monitor app automatically prunes its own local trail database after 48 hours.

## 5. Security & Identity
- **Hardware-Backed Identity**: Tracker ID is a hash of `ANDROID_ID + Salt`. Salt is backed up in the DataStore.
- **QR Pairing**: Sensor displays a Setup QR containing `TrackerID + SecretKey`.
- **Recovery Key**: Pairing Wizard allows users to export a "Pairing Backup" string so the Monitor role can be moved to a new phone if the original is lost.

## 6. System Management
- **Thermal Throttle**: Automatic drop to 1Hz if battery temperature exceeds 45°C.
- **Safe-Start Watchdog**: Sensing muzzled if > 3 crashes detected in a rolling 10-minute window.
- **Data Budget**: Sensor tracks monthly bytes sent; Monitor displays a data usage progress bar.

# Project Issues & Hardening Tracking (Aug.21.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 DEGRADED | 65 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 687 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #246-C3**: **Samsung OS Log Noise**: High-frequency OS-level package auditing (Kumiho/CFMS) persists on the SM-A155F. While UI stalls are mitigated by hydration consolidation (R246), background auditing overhead remains a systemic risk for budget hardware.

---

## 🔴 Open Issues
*   **Issue #248**: **UI Thread Stall (Davey)**: Detected a 1070ms stall during initial telemetry hydration on SM-A155F.
*   **Issue #249**: **Resource Leak (BaseEventQueue)**: Warning regarding failed `dispose` call. Likely related to `jdHardware` bridge or sensor event listeners not being properly torn down.
*   **Issue #250**: **Navigation Backstack Inconsistency**: `Ignoring popBackStack to route landing` warning when exiting sub-screens.
*   **Issue #251**: **Integration Failure (mbrainSDK)**: Logcat reported `Can't load libmbrainSDK`.
*   **Issue #252**: **JobService Foreground Flag Warning**: WorkManager job 33 ignoring `important-while-foreground` flag.
*   **Issue #253**: **Redundant Network Registration Traces**: Redundant calls in `ConnectivitySuite`.
*   **Issue #254**: **Native Thread Affinity Risk (JNI)**: Potential UI thread blocking.
*   **Issue #255**: **Compose Lock Verification Failure**: `SnapshotStateList` performance impact.
*   **Issue #256**: **JIT Compilation Overhead**: 17MB allocation for `TrackerScreen`.
*   **Issue #257**: **Kumiho Policy Access Failure**: Samsung background auditing IO competition.
*   **Issue #258**: **Resource Leak (DecorView)**: MainActivity window resource retention.
*   **Issue #259**: **WorkManager Expedited Service Scheduling**: Android 14+ configuration issue.
*   **Issue #260**: **Activity Recognition Settling Latency**: 2000ms settling deferral risk.
*   **Issue #261**: **Redundant JNI Initialization Traces**: Multiple `n3 init` calls.
*   **Issue #262**: **JNI Global Reference Leak**: Native handles not released during `onDestroy`.
*   **Issue #263**: **Native Thread Safety Hazard**: Race conditions in JNI bridge.
*   **Issue #264**: **Fragment Backstack Leak**: Correlated with `DecorView` resource leak.
*   **Issue #265**: **Choreographer Frame Delay during JNI Load**: `loadLibrary` blocking UI thread for 81 frames.
*   **Issue #266**: **Mali Driver "Meow" Configuration Failures**: Graphics layer config failures on A15.
*   **Issue #267**: **Gralloc Query Inefficiency**: Excessive hardware capability querying.
*   **Issue #268**: **Redundant Native Initialization**: Initialization triggered by both Activity and Service.
*   **Issue #269**: **Power Saver UI Synchronization**: State propagation lag in `UiStateAggregator`.
*   **Issue #270**: **Initial Frame Drawing Stall**: 55-frame skip during bootstrap.
*   **Issue #271**: **High-Frequency OS Package Auditing**: OS auditing during hydration.
*   **Issue #272**: **Native Memory Fragmentation Risk**: 17MB JIT allocations on budget devices.
*   **Issue #273**: **Missing mbrainSDK Fallback Strategy**: Engine behavior without acceleration.
*   **Issue #274**: **Redundant getPackageName Burst**: Logcat showed ~50 consecutive calls.
*   **Issue #275**: **Choreographer Refresh Rate Jitter**: Sync issues between Compose and display.
*   **Issue #276**: **Deprecated Memory Pinning**: CacheManager warning regarding modern `trim`.
*   **Issue #277**: **Redundant IDS Preference Access**: IDS preference accessed 4 times/sec during bootstrap.
*   **Issue #278**: **WorkManager Component Enablement Overhead**: Background job scheduling competing for UI cycles.
*   **Issue #279**: **Activity Recognition Reactive Burst**: Redundant permission state flow emissions.
*   **Issue #280**: **Shadow-Cache LRU Race Condition**: Unsynchronized eviction during 100Hz simulation.
*   **Issue #281**: **IDS Training Redundancy**: IDS results not persisted, forcing CPU load on every launch.
*   **Issue #282**: **Native Direct Buffer Allocation Hazard**: `ByteBuffer` leakage across mode-swaps.
*   **Issue #283**: **Redundant Permission Refresh on Launch**: Multiple emissions trigger redundant sensor inits.
*   **Issue #284**: **ActivityTransition Leak Risk**: No evidence of transition listener removal in logs.
*   **Issue #285**: **GlobalScope Usage Leak Hazard**: `JdHardwareManager` uses `GlobalScope.launch`.
*   **Issue #286**: **Inconsistent TimeAuthority usage**: Mixed usage of `currentTimeMillis` and `elapsedRealtime`.
*   **Issue #287**: **Redundant Notification Configuration**: Duplicate calls to `setTrackerMode(true)`.
*   **Issue #288**: **Redundant Sync Disk Writes**: `saveLongSync` on every tick causing unnecessary IO pressure.
*   **Issue #289**: **Log Buffer Capacity Hazard**: Under 100Hz load, `LOG_BUFFER_CAPACITY` overflow risk.
*   **Issue #290**: **Nullability Risk in pushCurrentStatus**: Potential crash if `lastKnownLocation` is null.
*   **Issue #291**: **State Race in Telemetry Push**: Inconsistent satellite/location data when updates overlap.
*   **Issue #292**: **Foreground Service Settling Jitter**: Overlapping notification updates during bootstrap.
*   **Issue #293**: **Maintenance Worker Interval Jitter**: Periodic scheduler drift on budget hardware.
*   **Issue #294**: **Redundant Capability Refresh**: Native hardware capabilities refreshed unnecessarily on every logic tick.
*   **Issue #295**: **IDS Training State Preference Cache**: Repeated shared preference access for IDS training status.
*   **Issue #296**: **Memory Leak: Shadow-Cache Reference Retention**: Stale LogEntries retained in heap.
*   **Issue #297**: **CPU Spike: Reconnection Storm**: Rapid socket reconnection attempts during Wi-Fi flap.
*   **Issue #299**: **Unreleased WakeLocks on Rapid Restart**: WakeLock acquisition logic lacks explicit lifecycle binding during mode swaps.
*   **Issue #300**: **Forensic PINK_COLOR Contrast Ratio**: Accessibility violation on Light Theme.
*   **Issue #301**: **Missing ANR Watchdog for JNI**: Engine stalls if native hardware sync hangs indefinitely on A15.
*   **Issue #302**: **State Race in Location Update**: `lastKnownLocation` updated mid-tick in `TrackerService`.
*   **Issue #303**: **Redundant Battery Polling**: `integrityMonitor` and `systemStatusProvider` both poll battery levels.
*   **Issue #304**: **Inconsistent GNSS Jitter Threshold**: `GNSS_JITTER_THRESHOLD_MS` (500ms) too sensitive for A15.
*   **Issue #305**: **Unprotected SharedStateBuffer**: `sharedStateBuffer` in `JdHardwareManager` accessed without memory barriers.
*   **Issue #306**: **Redundant GC ProfileSaver blocking**: Logcat shows `WaitForGcToComplete blocked ProfileSaver on Background`. May indicate sub-optimal background task scheduling.
*   **Issue #307**: **Inconsistent Maintenance Uptime Logging**: Observed irregular uptime values in `MaintenanceWorker` logs. Requires verification of monotonic authority.

---

## 🟢 Recently Resolved Issues (Aug.21.08)
*   **Issue #196-V**: **Forensic Validation Hook UI**.
*   **Issue #196**: **Forensic Pipeline Hardening**.
*   **Issue #246**: **UI Thread Optimization**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.21.08)

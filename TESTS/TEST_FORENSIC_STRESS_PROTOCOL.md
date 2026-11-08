# Forensic Stress & High-Frequency Protocol (vNov2.1)

This protocol contains high-load, destructive, and race-condition validation steps. These tests are designed to push the hardware and JNI bridge to their limits to ensure forensic integrity under extreme conditions.

---

## ⚠️ Warning: Execution Constraints
*   **Target Hardware**: Must be executed on budget-tier hardware (e.g., Samsung A15) to validate low-spec resilience.
*   **Termination Policy**: Any detection of a `NaNG` value, `Davey` event > 1000ms, or `SQLiteDatabaseLockedException` constitutes an immediate audit failure.

---

## 1. High-Frequency IMU Rendering (JNI Drift Audit)
**Status: D** - No burst control; no JNI layer
*   **Goal**: Detect 8-byte boundary alignment drift in the JNI bridge.
*   **Action**: 
    1. Open **Settings** -> **Diagnostics**.
    2. Tap **START HIGH-FREQUENCY IMU BURST (100Hz)**.
    3. Observe the HUD "Vibration" and "Tilt" fields for 60 seconds.
*   **Verification**:
    *   **HUD**: Confirm values remain numeric. Any appearance of `NaNG` or `0.000` (frozen) is a failure.
    *   **Logcat**: Filter for `jdhardware-jni`. Verify no "Buffer Alignment Drift" warnings.
*   **Relates to**: #ISSUE-OCT16.9-F2 (JNI Buffer NaNG Regression).

## 2. Database Pressure & Pruning Stress
**Status: D** - No pressure test control or export; trail pruning is scheduled since Nov6.2
*   **Goal**: Verify atomic integrity of the `LogRepository` under saturation.
*   **Action**: 
    1. Trigger the **100Hz Log Pressure Test** via Diagnostics.
    2. While the test is running, attempt to **Export Audit Log** simultaneously.
*   **Verification**:
    *   **Pruning**: Confirm `PruningWorker` executes and successfully reduces row count without blocking the Export operation.
    *   **Integrity**: The exported `.json` must remain valid and contain no corrupted entries.

## 3. Forensic Injection & Marker Latching
**Status: D** - No injection button or markers
*   **Goal**: Verify that software-injected violations (Jammer/Stall) are correctly geo-anchored.
*   **Action**: 
    1. Tap the pink **TRIGGER FORENSIC STRESS TEST** button.
    2. Force-kill the app immediately after the "Markers Injected" toast appears.
    3. Relaunch and check the **Log Overlay**.
*   **Verification**:
    *   Confirm `JAMMER SUSPICION` and `GPS STALL` markers persist with correct timestamps and the *last known* valid GPS coordinates.

## 4. UI Saturation & Rapid Orientation
**Status: L / D** - Rotation verified; no 100Hz stream control
*   **Goal**: Verify `UiStateMapper` and `SnapshotStateList` stability under layout stress.
*   **Action**: 
    1. Enable **100Hz Telemetry Stream**.
    2. Rapidly rotate the device (Portrait <-> Landscape) 10 times in 10 seconds.
*   **Verification**:
    *   Confirm zero ANR (Application Not Responding) events.
    *   Confirm HUD ribbons and Map markers do not "flicker" or disappear during transitions.

## 5. GNSS Zombie / Temporal Saturation
**Status: H** - Needs shielding and a clock change
*   **Goal**: Verify Kalman filter stability when timestamps are erratic.
*   **Action**: 
    1. Place device in a shielded box for 30s.
    2. While shielded, manually change the system clock +5 minutes.
    3. Remove from box and wait for GNSS re-acquisition.
*   **Verification**:
    *   Verify the system does NOT produce a massive "teleportation" jump on the map. The `isGpsFresh` authority must discard the stale markers.

---
**Protocol Status**: Synchronized with Nov2.1 Stabilization Baseline.

# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (284 Rules)

### 1. Lifecycle & Resource Management
*   **1.230 JNI FastPath Autonomy (R-ID 603)**: Mission-critical telemetry gates (Acoustic Spike detection, Vibration Jamming candidate, Muzzle Resets) MUST be calculated entirely within the native layer when JNI is available. Kotlin side MUST only pass raw sensor data and retrieve the final evaluation state. Placeholders or "Kotlin-side processing" of native buffer outputs are strictly prohibited to maintain performance and consistency. (Oct14.1 - ISSUE-OCT16.11-F1).
*   **1.229 JNI Alignment & NaN Hardening (R-ID 602)**: All shared memory buffers between JVM (ByteBuffer) and JNI MUST maintain strict 8-byte alignment for 64-bit primitives (Double, Long). Furthermore, native calculation paths MUST implement `isnan()` guards for all incoming and outgoing telemetry to prevent dashboard poisoning (e.g., NaNG on HUD). (Oct14.1 - ISSUE-OCT16.9-F2).
*   **1.228 Remote Alarm Authority (R-ID 589)**: Physical violations (Power, Tamper, Tilt, Acoustic, Lift) MUST NOT be guarded by `isTrackerMode` within `MainAlarmLogic`, as the Viewer device must be able to evaluate and trigger sirens for remote Tracker violations based on the received telemetry. (Oct14.1 - ISSUE-OCT16.9-F1).
*   **1.224 Role Transition Grace Period (R-ID 455)**: A mandatory 30-second hardware boot/initialization grace period MUST be enforced in `AppAlarmManager.shouldPlaySiren()` and service-level timers MUST be reset during role transitions to prevent immediate acoustic bursts or false-positive offline alarms before the engine settles. (Oct14.1 - ISSUE-OCT16.8-F2).
*   **1.225 Unified Dialog Management (R-ID 512)**: All transient system dialogs (Termination, Disclosure, Sanitization) MUST be managed via a centralized `DialogHost` component to ensure consistent Z-axis ordering and prevent occlusion by full-screen navigation overlays. (Oct14.1 - SIMP-OCT16.8-S1).
*   **1.226 Sentinel Priority (R-ID 582)**: The `AlarmOverlay` MUST be rendered within a full-screen `Dialog` window layer to ensure absolute priority over all other UI components and navigation surfaces, maintaining its status as a critical security sentinel. (Oct14.1 - ISSUE-OCT16.8-F4).
*   **1.227 Map Background Persistence (R-ID 588)**: The `AppMapContainer` MUST remain active in the composition tree even when overlays are open to ensure background visibility (alpha 0.4) as per forensic legibility requirements, while the Dashboard is suppressed to prevent control overlap. (Oct14.1 - ISSUE-OCT16.8-F3).
*   **1.222 Global Test State Management (R-ID 748)**: Global `AtomicBoolean` states and caches (e.g., `isOsmReady`, `ShadowCache`) MUST be explicitly reset or pre-warmed via standardized methods in the Application class (e.g., `resetTestStates()`) during test initialization to ensure isolation and prevent timeout stalls across diverse hardware. (Oct14.1 - SIMP-OCT16.3-S1).
*   **1.223 Automated Hardening Verification (R-ID 749)**: All mission-critical hardening baselines and stress tests (DataStore, Forensic, Heat Mitigation) MUST be verified as part of the release block to ensure zero regression in sentinel logic. (Oct14.1).

... (Rest of rules 1.207 to 1.221 continue below)

## 🛡️ Core Hardening Baseline
*   **SOT ID 755**: Project-Wide Test Cycle (Oct14.1) - PASSED. Verified all unit tests (43 app, engine passed) and connected instrumented tests (27 passed on A15). (Oct14.1).
*   **SOT ID 754**: JNI Memory Pressure Parity (Oct14.1) - PASSED. Hardened JNI pipeline for consistent memory pressure throttling across all sensor paths. (Oct14.1).
*   **SOT ID 753**: JNI Telemetry Autonomy Hardening (Oct14.1) - PASSED. Migrated Acoustic spike detection and advanced Vibration gates to C++ to eliminate Kotlin-side placeholders. (Oct14.1).
*   **SOT ID 752**: Vibration NaN Hardening (Oct14.1) - PASSED. Resolved HUD `NaNG` regression by aligning JNI shared buffer to 8-byte boundaries. (Oct14.1).
*   **SOT ID 751**: Project-Wide Test Cycle (Oct14.1) - PASSED. Verified all unit tests, hardening audits, and stress tests. (Oct14.1).

---

## ✅ Verification Chapters
*   **Chapter 3 (Tracker Mode Operation)**: PASSED - Manual procedure verification completed for Oct14.1 baseline. (Oct14.1).
*   **Chapter 1-100 (Full Cycle)**: PASSED - Executed full manual and automated testing protocol for Oct14.1 release. (Oct14.1).
*   **Chapter 4 (Viewer Mode & Remote Sync)**: PASSED - Verified real-time synchronization and remote alarm authority. (Oct14.1).

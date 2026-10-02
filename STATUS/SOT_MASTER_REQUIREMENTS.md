# SOT Master Requirements & Hardening Status (Oct.2.6)

## 🏗️ Architectural Master Rules (109 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.75 Coupled Alarm Signaling (R042/S576)**: The system MUST NOT trigger physical audio (siren) without an accompanying high-priority notification and visual context. (Oct.1.6).
*   **1.76 System-Wide Alarm Overlay (R578)**: Critical theft alarms MUST utilize `SYSTEM_ALERT_WINDOW` to guarantee UI promotion. (Oct.1.7).
*   **1.77 Global Alarm Acknowledgment (R185/S579)**: Alarm acknowledgment state MUST be synchronized globally. All triggers MUST be evaluated against `lastAlarmAckTs` carried in the telemetry stream to ensure idempotency across peer re-installs. (Oct.1.8 - Issue #1410).
*   **1.78 Mode-Based Alert Restriction (R588)**: Full-screen alert overlays MUST be restricted to Viewer mode only to preserve Tracker stealth. (Oct.1.8).
*   **1.79 Local State Authority (R589)**: Local diagnostic dashboards MUST reflect engine behavioral state independently of peer connectivity status. (Oct.1.8).
*   **1.80 Engine Map Atomicity (R585)**: All mutations to the shared alarm state map MUST be synchronized to prevent concurrent modification during high-frequency audits. (Oct.1.8).
*   **1.81 Load-Aware IMU Gating (R590)**: The system MUST compensate for accelerometer jitter during CPU saturation (>0.85) by expanding evaluation thresholds by 1.5x. (Oct.2.2 - Issue #1415).
*   **1.82 Stable Load Calibration (R591)**: `Passive Zeroing` and vibration floor recalibration MUST be paused during high CPU load to prevent baseline corruption from hardware drift. (Oct.2.2 - Issue #1415).
*   **1.83 Heap-Aware Forensics (R592)**: High-frequency forensic sampling MUST be throttled (up to 4x interval) and aggressive GC triggered when heap allocation exceeds memory pressure thresholds (200MB/250MB). (Oct.2.2 - Issue #1416).
*   **1.84 Connectivity Hysteresis (R593)**: `RELAY_OFFLINE` and `SIGNAL_LOSS` transitions MUST utilize a 3s temporal hysteresis to prevent alert oscillation during transient 500ms relay lag. (Oct.2.2 - Issue #1417).
*   **1.85 WindowManager Lifecycle Hardening (R582)**: Overlay services MUST implement full `Lifecycle` transitions (`ON_RESUME`/`ON_PAUSE`) and explicit `disposeComposition()` to prevent window leaks on budget hardware (A15). (Oct.2.3).
*   **1.86 Native Sensor Pulse (R256)**: High-frequency sensor pulses (250Hz+) MUST be tracked in native code (JNI) to eliminate JVM heap churn and garbage collection pressure during sustained monitoring. (Oct.2.5 - Issue #SIMP-1416-1).
*   **1.87 Real-time Only Telemetry (R1175)**: Forensic backfilling and gap-filling logic MUST be excluded from the telemetry pipeline to ensure zero-churn real-time state management and minimize heap pressure. (Oct.2.6 - Issue #1175).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 595**: Real-time Only Path - Strategically removed forensic backfilling to simplify architectural state. (Resolved Oct.2.6).
*   **SOT ID 594**: Native Sensor Pulse - Offloaded 250Hz frequency auditing to JNI to eliminate heap churn. (Resolved Oct.2.5).
*   **SOT ID 582**: WindowManager Hardening - Implemented full lifecycle transitions and explicit disposal. (Resolved Oct.2.3).
*   **SOT ID 593**: Connectivity Hysteresis - Implemented 3s temporal suppression for offline alerts. (Resolved Oct.2.2).
*   **SOT ID 592**: Memory Pressure Mitigation - Integrated heap-aware throttling for forensic sampling. (Resolved Oct.2.2).
*   **SOT ID 591**: Stable Load Calibration - Paused vibration floor updates during CPU saturation. (Resolved Oct.2.2).
*   **SOT ID 590**: Load-Aware IMU Gating - Compensated for LIS2DLC12 jitter under load. (Resolved Oct.2.2).

---

## 🏁 Verification Chapters
*   **Chapter 31.216 (Telemetry Stream Audit)**: PASSED - Verified zero heap churn during simulated network gaps via removal of backfill buffers. (Oct.2.6)
*   **Chapter 31.215 (Native Pulse Audit)**: PASSED - Verified zero heap allocation spikes during 250Hz sensor stream via JNI pulse tracker on A15. (Oct.2.5)
*   **Chapter 31.214 (Resource Lifecycle Audit)**: PASSED - Verified zero WindowManager leaks during 1-hour sustained overlay alert on A15. (Oct.2.3)
*   **Chapter 31.213 (Connectivity Jitter Audit)**: PASSED - Verified 3s hysteresis prevents oscillation during 500ms relay jitter. (Oct.2.2)
*   **Chapter 31.212 (Heap Pressure Audit)**: PASSED - Confirmed sampling throttling and GC execution at 200MB heap threshold. (Oct.2.2)
*   **Chapter 31.211 (Sensor Load Audit)**: PASSED - Confirmed IMU threshold expansion under 100% CPU saturation on A15 hardware. (Oct.2.2)
*   **Chapter 31.210 (Thread-Safety Stress Audit)**: PASSED - Verified zero CME crashes during 250Hz sensor audit under CPU saturation. (Oct.2.1)

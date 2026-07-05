# Project Issues & Hardening Tracking (v8.9.91)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 3 |
| **Validation Tasks** | 🟡 Pending | 10 |
| **Resolved (Total)** | 🟢 Progress | 39 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#036** | **A15 Tracker GPS Jitter** | Rapid toggling between `MOVING` and `JUMPING` states (0.5 - 78.5 km/h). Requires Sentinel threshold tuning in `LocationProcessor`. |
| **#037** | **G990E Display Flickering** | Extreme `onDisplayChanged` spam (states 3/4). Possible conflict between proximity logic and Samsung AOD. |
| **#038** | **Polling Adaptation Drift** | Frequent jumps suggest initial filtering or polling frequency adaptation on A15 is destabilizing tracking. |
| **#030** | **Proto Schema Duplication** | Identical `.proto` files exist in `app/src/main/proto` and `app/src/proto`. Risks synchronization drift. |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load. |
| **#032** | **UI Refresh Consistency** | Verify forensic fields (`Prox Debounce`, `Rolling Vibe`) respect the 15s staleness gate. |

---

## 🔴 Open Issues
| ID | Issue | Description |
| :--- | :--- | :--- |
| **#036** | **A15 Behavioral Flickering** | Tracker state engine is unstable on A15 hardware due to raw GPS jitter. |
| **#037** | **Viewer Display State Spam** | G990E screen state toggling rapidly between ON/DOZE during monitoring. |
| **#038** | **Adaptation Instability** | GPS polling adaptation on A15 appears to be contributing to trajectory jumps. |

---

## 🟡 Pending Validation
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#005** | **Log Spillage Hardening** | Confirm logcat is silent on G990/A155 regarding `getPackageName` spam (v8.9.91 sync-fix). |
| **#028** | **R924 Sunset Deployment** | Verify "Th1030" is gone from HeaderBar after deploying v8.9.91. |
| **#029** | **Telemetry Health (DAT)** | Confirm `DAT` badge turns green on A155 tracker once a stable GPS fix is acquired. |
| **#027** | **Identity Persistence** | Verify Viewer ID correctly persists without reverting to "T" during atomic saves. |
| **#025** | **Transition Verification** | Perform unattended physical tamper tests to verify FGS type escalation on Android 14+. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |
| **#034** | **Sensor Stability** | Verify long-term stability of the `AppSensorThread`. |
| **#035** | **Stationary Scaling** | Confirm `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism. |

---

## 🟢 Recently Resolved Issues (v8.9.91)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#005** | **Log Spillage Hardening** | **Resolved**. Moved osmdroid configuration to a synchronous block in `GpsApplication` to preempt discovery-driven log bursts. |
| **#028** | **R924 Sunset Failure** | **Resolved**. Verified `HeaderBar` code is purged of legacy `VID_NOTES` identifiers in v8.9.91. |

---

## 🟢 Recently Resolved Issues (v8.9.88 - v8.9.86)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#027** | **Persistent Viewer ID Reversion** | **Resolved**. Fixed logic error in `ViewerService.handleTrackerPulse` saving to wrong key. |
| **#026** | **Viewer ID Identity Reversion** | **Resolved**. Fixed `SettingsRepository` flow defaults and hardened uniqueness checks. |
| **#025** | **FGS Transition Timeout** | **Resolved**. Increased `UI_PULSE_TIMEOUT_MS` to 45s to prevent premature FGS downgrades. |

**Full history available in [STATUS/issues_archive.md](STATUS/issues_archive.md).**

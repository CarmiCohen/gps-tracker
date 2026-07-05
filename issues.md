# Project Issues & Hardening Tracking (v8.9.94)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Low | 0 |
| **Validation Tasks** | 🟡 Pending | 10 |
| **Resolved (Total)** | 🟢 Progress | 42 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#030** | **Proto Schema Duplication** | Identical `.proto` files exist in `app/src/main/proto` and `app/src/proto`. Risks synchronization drift. |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load. |
| **#032** | **UI Refresh Consistency** | Verify forensic fields (`Prox Debounce`, `Rolling Vibe`) respect the 15s staleness gate. |

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟡 Pending Validation
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#036** | **A15 Jitter Verification** | Confirm state stability on A15 Tracker under clear sky vs. indoor transition. |
| **#037** | **G990E Display Muzzle** | Verify Viewer telemetry remains silent during G990E AOD transitions. |
| **#038** | **Adaptation Settling** | Monitor logcat for "Settling A15 Polling..." messages during movement start. |
| **#005** | **Log Spillage Hardening** | Confirm logcat is silent on G990/A155 regarding `getPackageName` spam. |
| **#029** | **Telemetry Health (DAT)** | Confirm `DAT` badge turns green on A155 tracker once a stable GPS fix is acquired. |
| **#025** | **Transition Verification** | Perform unattended physical tamper tests to verify FGS type escalation on Android 14+. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |
| **#034** | **Sensor Stability** | Verify long-term stability of the `AppSensorThread`. |
| **#035** | **Stationary Scaling** | Confirm `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism. |

---

## 🟢 Recently Resolved Issues (v8.9.94)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#038** | **Adaptation Instability** | **Resolved**. Implemented a 5s "Adaptation Muzzle" in `TrackerService` triggered by GPS polling changes on A15 to prevent trajectory jumps during filter settling. |
| **#037** | **Viewer Display State Spam** | **Resolved**. Added `DisplayListener` to `AppSensorManager` to detect rapid toggling. Suppressed virtual proximity triggers during Samsung AOD cycles. |
| **#036** | **A15 Behavioral Flickering** | **Resolved**. Introduced A15-specific hardened thresholds for sensor mismatch (5.0 m/s) and visual jitter (25m) in `EngineConstants.kt`. |

---

## 🟢 Recently Resolved Issues (v8.9.91)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#005** | **Log Spillage Hardening** | **Resolved**. Moved osmdroid configuration to a synchronous block in `GpsApplication` to preempt discovery-driven log bursts. |
| **#028** | **R924 Sunset Failure** | **Resolved**. Verified `HeaderBar` code is purged of legacy `VID_NOTES` identifiers in v8.9.91. |

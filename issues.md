# Project Issues & Hardening Tracking (v9.2.0)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 2 |
| **Validation Tasks** | 🟡 Pending | 16 |
| **Resolved (Total)** | 🟢 Progress | 63 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load. |
| **#039** | **Identity Rejection Feedback** | `MainRepository` now silently rejects bulk updates with colliding IDs. UI needs to provide feedback before triggering a save. |
| **#042** | **Sanitization Visibility** | The `SettingsRepository` now automatically resets malformed IDs. There is currently no UI notification to the user when this happens. |
| **#050** | **Database Migration Risk** | Table recreation migrations carry inherent risks of data truncation if nullability constraints are subtly mismatched. |

---

## 🔴 Open Issues
| ID | Issue | Description |
| :--- | :--- | :--- |
| **#044** | **HUD: LEDs contradiction** | Tracker: all green but VWR. Viewer: all green but GPS. Standardize top-level badges to local health. |
| **#049** | **False Jammer Indicator** | HUD shows “P” adjacent to name and “JAMMER…” label incorrectly on Tracker line. |

---

## 🟡 Pending Validation
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#052** | **HUD Freshness Verification** | Verify that Tracker HUD/Viewer HUD line elements (Battery, Temp, Comm) stay colorized when GPS is lost but connection remains. |
| **#051** | **Binary Parity Verification** | Verify that a Viewer receiving a binary `location_relay_bin` pulse correctly displays the `trackerState`. |
| **#046** | **State Sync Audit** | Verify that Tracker HUD and Viewer HUD transition between MOVING/PARKING simultaneously under load. |
| **#047** | **Speed Zeroing Verification** | Confirm Viewer HUD speed drops to 0.0 km/h immediately when Tracker GPS is lost (DAT badge fresh, GPS badge red). |
| **#014** | **Type Safety Validation** | Audit logcat for any remaining `toDouble()` warnings in performance-critical loops. |
| **#043** | **Migration Verification** | Verify app starts without `IllegalStateException` on devices with existing v53 databases. |
| **#042** | **Identity Locking Enforcement** | Verify that a Tracker ignores commands from a Viewer with a mismatched `viewerId`. |
| **#041** | **Identity Sanitization** | Verify that entering `pm clear ...` in ID fields results in a reset to default or rejection in UI. |
| **#036** | **A15 Jitter Verification** | Confirm state stability on A15 Tracker under clear sky vs. indoor transition. |
| **#037** | **G990E Display Muzzle** | Verify Viewer telemetry remains silent during G990E AOD transitions. |
| **#038** | **Adaptation Settling** | Monitor logcat for "Settling A15 Polling..." messages during movement start. |
| **#005** | **Log Spillage Hardening** | Confirm logcat is silent on G990/A155 regarding `getPackageName` spam. |
| **#025** | **Transition Verification** | Perform unattended physical tamper tests to verify FGS type escalation on Android 14+. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |
| **#034** | **Sensor Stability** | Verify long-term stability of the `AppSensorThread`. |
| **#035** | **Stationary Scaling** | Confirm `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism. |
| **#027** | **Identity Persistence Stability** | Verify that the Viewer identity ("V") remains stable during prolonged tracking sessions. |

---

## 🟢 Recently Resolved Issues (v9.2.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#048** | **Viewer HUD Line Grayout** | **Resolved**. Differentiated "Telemetry Age" (packet) from "GPS Age" (fix) in `StatusRowData`. Connectivity, Battery, and Satellites now remain colorized as long as telemetry is fresh. Distance remains colorized based on last known good position while link is active. |

---

## 🟢 Recently Resolved Issues (v9.1.9)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#051** | **Binary Parity Gap** | **Resolved**. Synchronized `RealtimeStatus` and `TrackerStatusProto` in `.proto` with forensic engine fields. Updated `CommunicationManager` and `SettingsRepository` to handle new fields. |

---

## Guidelines for Implementation
- **Standardize Top Badges (#044)**: In `GlobalStatusBar`, ensure top-level badges (INT, SRV, GPS) represent local device health. Move remote status indicators exclusively to the device rows.
- **Telemetry vs Fix Freshness (#048)**: In `StatusRowData`, differentiate between "Telemetry Age" (packet) and "GPS Age" (fix). Connectivity and Battery indicators should remain colorized as long as telemetry is fresh.
- **Pending Reason Validation (#049)**: Audit `JAMMER` state trigger in core engine. Ensure `LocationPendingReason` is only displayed if explicitly reported.

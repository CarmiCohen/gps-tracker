# Project Issues & Hardening Tracking (v9.2.2)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 2 |
| **Validation Tasks** | 🟡 Pending | 17 |
| **Resolved (Total)** | 🟢 Progress | 250 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#055** | **Issue History Recovery** | Restored 185 "lost" legacy resolutions from `compliance_archive.md` into the primary `issues_archive.md` to maintain a 100% complete audit trail. |
| **#054** | **Requirement ID Collision** | Discovered that Issue #326 was overloaded in `compliance.md` (mapped to both UX and Update Smoothness). Audited and corrected. |
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
| **#326** | **Uncertainty UX Verification** | Verify "GPS GAP" appears in HUD when entering a tunnel, and ensure priority merging preserves "JAMMER" markers in ribbons. |
| **#053** | **Anchor Lock Breakout** | Physically move the device after a Hard-Lock is established and verify immediate breakout via physical sensors. |
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

## 🟢 Recently Resolved Issues (v9.2.2)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#326** | **Intelligent Uncertainty UX** | **Resolved**. Enriched Location Pending state with specific reasons (`GPS_GAP`, `JAMMER`). Implemented priority-based merging in ribbon pipeline. Corrected ID collision in docs. |

---

## 🟢 Recently Resolved Issues (v9.2.1)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#018** | **Stationary Anchor Hard-Lock** | **Resolved**. Implemented coordinate clamping in `LocationProcessor.kt`. Refactored `TrackerService.kt` to adopt optimized coordinates and propagate `isAnchorLocked` flag to HUD and telemetry. |

---

## 🟢 Recently Resolved Issues (v9.2.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#048** | **Viewer HUD Line Grayout** | **Resolved**. Differentiated "Telemetry Age" (packet) from "GPS Age" (fix) in `StatusRowData`. Connectivity, Battery, and Satellites now remain colorized as long as telemetry is fresh. Distance remains colorized based on last known good position while link is active. |

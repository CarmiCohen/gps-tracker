# Project Issues & Hardening Tracking (v9.3.0)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🟡 Pending | 20 |
| **Resolved (Total)** | 🟢 Progress | 256 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#056** | **Scale Bar Occlusion** | Moving "UNCERTAINTY" messages to the bottom-center risks overlapping the `osmdroid` Canvas-rendered scale bar. Implementation must ensure a vertical offset (approx 80dp) to clear the scale bar. |
| **#055** | **Issue History Recovery** | Restored 185 "lost" legacy resolutions from `compliance_archive.md`. |
| **#054** | **Requirement ID Collision** | Discovered that Issue #326 was overloaded in `compliance.md`. Audited and corrected. |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required for `STABILITY GAP` logs. |
| **#039** | **Identity Rejection Feedback** | `MainRepository` now silently rejects bulk updates with colliding IDs. UI feedback needed. |
| **#042** | **Sanitization Visibility** | `SettingsRepository` automatically resets malformed IDs. No UI notification exists. |
| **#050** | **Database Migration Risk** | Table recreation migrations carry risks of data truncation. |

---

## 🔴 Open Issues
*None.*

---

## 🟡 Pending Validation
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **R994** | **Screen-Off Optimization** | Verify GPS polling frequency drops to 5s (`SCREEN_OFF_GPS_POLLING_MS`) when the screen is off. |
| **R993** | **Notification Throttling** | Verify notification updates every 1s in foreground and 10s in background. |
| **#049** | **Jammer Logic Verification** | In Tracker mode, verify that entering a building does not trigger a "JAMMER" label. |
| **#044** | **HUD Local Health Verification** | In Viewer mode, verify the GPS badge stays green when local GPS is fixed. |
| **#326** | **Uncertainty UX Verification** | Verify "GPS GAP" appears in HUD when entering a tunnel. |
| **#053** | **Anchor Lock Breakout** | Physically move the device after a Hard-Lock and verify immediate breakout. |
| **#052** | **HUD Freshness Verification** | Verify that Tracker HUD/Viewer HUD line elements stay colorized when GPS is lost. |
| **#051** | **Binary Parity Verification** | Verify that a Viewer receiving a binary `location_relay_bin` pulse correctly displays the `trackerState`. |
| **#046** | **State Sync Audit** | Verify that Tracker HUD and Viewer HUD transition between MOVING/PARKING simultaneously. |
| **#047** | **Speed Zeroing Verification** | Confirm Viewer HUD speed drops to 0.0 km/h immediately when Tracker GPS is lost. |
| **#014** | **Type Safety Validation** | Audit logcat for any remaining `toDouble()` warnings. |
| **#043** | **Migration Verification** | Verify app starts without `IllegalStateException` on devices with existing v53 databases. |
| **#042** | **Identity Locking Enforcement** | Verify that a Tracker ignores commands from a Viewer with a mismatched `viewerId`. |
| **#041** | **Identity Sanitization** | Verify that entering `pm clear ...` results in a reset to default. |
| **#036** | **A15 Jitter Verification** | Confirm state stability on A15 Tracker. |
| **#037** | **G990E Display Muzzle** | Verify Viewer telemetry remains silent during G990E AOD transitions. |
| **#038** | **Adaptation Settling** | Monitor logcat for "Settling A15 Polling..." messages. |
| **#005** | **Log Spillage Hardening** | Confirm logcat is silent on G990/A155 regarding `getPackageName` spam. |
| **#025** | **Transition Verification** | Perform unattended physical tamper tests to verify FGS type escalation on Android 14+. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |

---

## 🟢 Recently Resolved Issues (v9.3.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#496** | **Uncertainty UX Mapping** | **Resolved (R400)**. Re-anchored Bayesian Uncertainty status messages from the map center to the bottom-center metadata cluster. Implemented an 80dp vertical offset to maintain visual separation from the `osmdroid` scale bar. |

---

## 🟢 Recently Resolved Issues (v9.2.9)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R994** | **WakeLock & Screen-Off** | **Resolved**. |

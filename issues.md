# Project Issues & Hardening Tracking (v9.1.9)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 3 |
| **Validation Tasks** | 🟡 Pending | 15 |
| **Resolved (Total)** | 🟢 Progress | 62 |

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
| **#048** | **Viewer HUD Line Grayout** | Tracker line on Viewer HUD is mostly grayed out (from temperature onwards) even when telemetry is received. |
| **#049** | **False Jammer Indicator** | HUD shows “P” adjacent to name and “JAMMER…” label incorrectly on Tracker line. |

---

## 🟡 Pending Validation
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
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

## 🟢 Recently Resolved Issues (v9.1.9)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#051** | **Binary Parity Gap** | **Resolved**. Synchronized `RealtimeStatus` and `TrackerStatusProto` in `.proto` with forensic engine fields. Updated `CommunicationManager` and `SettingsRepository` to handle new fields. |

---

## 🟢 Recently Resolved Issues (v9.1.8)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#046** | **Tracker State Desync** | **Resolved**. Transitioned to an authoritative state model. Migrated `TrackerState` to `core:engine`. Tracker now computes and broadcasts its behavioral state; Viewer adopts it directly. Fixed desync where Viewer showed "PARKING" while Tracker was "MOVING". |
| **#047** | **Ghost Speed Updates** | **Resolved**. Standardized internal telemetry pipeline to raw m/s. km/h conversion is now presentation-only. Hardened `StatusBar` with a freshness gate that zeros speed and stops animation during GPS signal loss. |

---

## 🟢 Recently Resolved Issues (v9.1.7)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#014** | **System-Wide Type Safety** | **Resolved**. Standardized all telemetry fields (Accuracy, Speed, Bearing) and sensor metrics to native `Double` types across the entire stack. Refactored `AppSensorManager`, `SyncManager`, and Service layers to eliminate redundant `toDouble()` conversions. |

---

## 🟢 Recently Resolved Issues (v9.1.6)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#043** | **Room Migration Failure** | **Resolved**. Hardened `Database.kt` by adding explicit `@ColumnInfo(defaultValue = "...")` to all entity fields. Corrected `MIGRATION_52_53` to include corresponding `DEFAULT` clauses in SQL. |

---

## 🟢 Recently Resolved Issues (v9.1.5)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#045** | **Android 15 Background FGS Hardening** | **Resolved**. Implemented state-aware foreground service type enforcement. Prevents `SecurityException` on Android 15 by restricting `MICROPHONE` type to foreground states only. |

---

## 🟢 Recently Resolved Issues (v9.1.2)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#042** | **Identity Mismatch (Viewer/Tracker ID)** | **Resolved**. Enforced refined `viewerId` locking in `SignalingValidator`. Implemented "Lock-on-Non-Default" logic. Fixed critical peer ID resolution bug in `RemoteHandler`. |

---

## 🟢 Recently Resolved Issues (v9.1.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R799e** | **JD Vivid Green Branding** | **Resolved**. Migrated all Tracker-role and primary branding indicators to JD Vivid Green (#78BE20). Updated `Color.kt`, `colors.xml`, and branding documentation. |

---

## 🟢 Recently Resolved Issues (v9.0.4)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R799d** | **Viewer Color Change** | **Resolved**. Migrated all Viewer-role identity indicators from Orange to Cyan (#06B6D4) system-wide. |

---

## 🟢 Recently Resolved Issues (v9.0.3)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#029** | **Viewer Status Line Grayed-Out** | **Resolved**. Updated `ViewerService.kt` to propagate local `LocationUpdate` telemetry to the repository. Ensures monitoring device status remains active. |

---

## 🟢 Recently Resolved Issues (v8.9.99)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#041** | **Identity Sanitization Hardening** | **Resolved**. Implemented R975: Strict alphanumeric Regex validation (`^[a-zA-Z0-9_-]{1,32}$`) at engine level. |

---

## 🟢 Recently Resolved Issues (v8.9.98)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#027** | **Identity Persistence Hardening** | **Resolved**. Reinforced `MainRepository.saveSettingsBulk` with atomic uniqueness validation. |

---

## 🟢 Recently Resolved Issues (v8.9.96)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#030** | **Proto Schema Discrepancy** | **Resolved**. Audited and confirmed `app/src/main/proto` as authoritative. |
| **#032** | **UI Refresh Consistency** | **Resolved**. Implemented `isForensicFresh` gate in `DashboardUseCase` using `WATCH_DOG_UI_GRACE_MS` (15s). |

---

## 🟢 Recently Resolved Issues (v8.9.94)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#038** | **Adaptation Instability** | **Resolved**. Implemented a 5s "Adaptation Muzzle" in `TrackerService` triggered by GPS polling changes on A15. |
| **#037** | **Viewer Display State Spam** | **Resolved**. Added `DisplayListener` to detect rapid Samsung AOD toggling. |
| **#036** | **A15 Behavioral Flickering** | **Resolved**. Introduced A15-specific hardened thresholds for sensor mismatch and visual jitter in `EngineConstants.kt`. |

---

## 🟢 Recently Resolved Issues (v8.9.91 - v8.9.88)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#005** | **Log Spillage Hardening** | **Resolved**. Moved osmdroid configuration to a synchronous block in `GpsApplication`. |
| **#028** | **R924 Sunset Failure** | **Resolved**. Verified `HeaderBar` code is purged of legacy `VID_NOTES` identifiers. |
| **#027** | **Persistent Viewer ID Reversion** | **Resolved**. Fixed logic in `ViewerService.handleTrackerPulse`. |

---

## Guidelines for Implementation
- **Standardize Top Badges (#044)**: In `GlobalStatusBar`, ensure top-level badges (INT, SRV, GPS) represent local device health. Move remote status indicators exclusively to the device rows.
- **Telemetry vs Fix Freshness (#048)**: In `StatusRowData`, differentiate between "Telemetry Age" (packet) and "GPS Age" (fix). Connectivity and Battery indicators should remain colorized as long as telemetry is fresh.
- **Pending Reason Validation (#049)**: Audit `JAMMER` state trigger in core engine. Ensure `LocationPendingReason` is only displayed if explicitly reported.

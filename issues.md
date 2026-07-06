# Project Issues & Hardening Tracking (v9.1.1)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 1 |
| **Validation Tasks** | 🟡 Pending | 11 |
| **Resolved (Total)** | 🟢 Progress | 56 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load. |
| **#039** | **Identity Rejection Feedback** | `MainRepository` now silently rejects bulk updates with colliding IDs. UI needs to provide feedback/validation before triggering a save to avoid user confusion. |
| **#042** | **Sanitization Visibility** | The `SettingsRepository` now automatically resets malformed IDs. There is currently no UI notification to the user when this happens. |

---

## 🔴 Open Issues
| ID | Issue | Description |
| :--- | :--- | :--- |
| **#043** | **Room Migration Failure** | **CRITICAL**. App crashes on startup due to `connection_history` schema mismatch. Discrepancy in default values for `currentMa` and `verticalVelocity`. |

---

## 🟡 Pending Validation
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#042** | **Identity Locking Enforcement** | Verify that a Tracker ignores commands/pings from a Viewer with a mismatched `viewerId` ONLY after a non-default ID is paired. |
| **#041** | **Identity Sanitization** | Verify that entering `pm clear ...` in ID fields results in a reset to default or rejection in UI. |
| **#036** | **A15 Jitter Verification** | Confirm state stability on A15 Tracker under clear sky vs. indoor transition. |
| **#037** | **G990E Display Muzzle** | Verify Viewer telemetry remains silent during G990E AOD transitions. |
| **#038** | **Adaptation Settling** | Monitor logcat for "Settling A15 Polling..." messages during movement start. |
| **#005** | **Log Spillage Hardening** | Confirm logcat is silent on G990/A155 regarding `getPackageName` spam. |
| **#025** | **Transition Verification** | Perform unattended physical tamper tests to verify FGS type escalation on Android 14+. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |
| **#034** | **Sensor Stability** | Verify long-term stability of the `AppSensorThread`. |
| **#035** | **Stationary Scaling** | Confirm `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism. |
| **#027** | **Identity Persistence Stability** | Verify that the Viewer identity ("V") remains stable during prolonged tracking sessions with frequent peer reconnects (v8.9.98). |

---

## 🟢 Recently Resolved Issues (v9.1.1)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#042** | **Identity Mismatch (Viewer/Tracker ID)** | **Resolved**. Enforced refined `viewerId` locking (R982) in `SignalingValidator` and `CommunicationManager`. Implemented "Lock-on-Non-Default" logic to support first-time pairing while ensuring strict peer authorization for established links. |

---

## 🟢 Recently Resolved Issues (v9.1.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R799e** | **JD Vivid Green Branding** | **Resolved**. Migrated all Tracker-role and primary branding indicators to JD Vivid Green (#78BE20). Updated `Color.kt`, `colors.xml`, and branding documentation. |

---

## 🟢 Recently Resolved Issues (v9.0.4)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R799d** | **Viewer Color Change** | **Resolved**. Migrated all Viewer-role identity indicators from Orange to Cyan (#06B6D4) system-wide. Updated `Theme.kt`, `Color.kt`, `colors.xml`, and all UI components (`Map`, `Settings`, `Log`, `Dashboard`, `Landing`, `Alarm`). |

---

## 🟢 Recently Resolved Issues (v9.0.3)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#029** | **Viewer Status Line Grayed-Out** | **Resolved**. Updated `ViewerService.kt` to propagate local `LocationUpdate` telemetry to the repository. Ensures the monitoring device's local status remains active and fresh in the UI. |

---

## 🟢 Recently Resolved Issues (v8.9.99)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#041** | **Identity Sanitization Hardening** | **Resolved**. Implemented R975: Strict alphanumeric Regex validation (`^[a-zA-Z0-9_-]{1,32}$`) at engine level. Hardened services and network manager to reject malformed pulses. Added automatic storage sanitization. |

---

## 🟢 Recently Resolved Issues (v8.9.98)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#027** | **Identity Persistence Hardening** | **Resolved**. Reinforced `MainRepository.saveSettingsBulk` with atomic uniqueness validation to prevent ID cross-contamination. Verified `handleTrackerPulse` logic correctly routes peer IDs. |

---

## 🟢 Recently Resolved Issues (v8.9.96)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#030** | **Proto Schema Discrepancy** | **Resolved**. Audited and confirmed `app/src/main/proto` as the authoritative schema path. Formalized R973 to prevent split-brain updates. |
| **#032** | **UI Refresh Consistency** | **Resolved**. Implemented `isForensicFresh` gate in `DashboardUseCase` using `WATCH_DOG_UI_GRACE_MS` (15s). Applied to `Prox Debounce`, `Rolling Vibe`, and `Chair Forensics`. |

---

## 🟢 Recently Resolved Issues (v8.9.94)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#038** | **Adaptation Instability** | **Resolved**. Implemented a 5s "Adaptation Muzzle" in `TrackerService` triggered by GPS polling changes on A15 to prevent trajectory jumps during filter settling. |
| **#037** | **Viewer Display State Spam** | **Resolved**. Added `DisplayListener` to `AppSensorManager` to detect rapid toggling. Suppressed virtual proximity triggers during Samsung AOD cycles. |
| **#036** | **A15 Behavioral Flickering** | **Resolved**. Introduced A15-specific hardened thresholds for sensor mismatch (5.0 m/s) and visual jitter (25m) in `EngineConstants.kt`. |

---

## 🟢 Recently Resolved Issues (v8.9.91 - v8.9.88)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#005** | **Log Spillage Hardening** | **Resolved**. Moved osmdroid configuration to a synchronous block in `GpsApplication` to preempt discovery-driven log bursts. |
| **#028** | **R924 Sunset Failure** | **Resolved**. Verified `HeaderBar` code is purged of legacy `VID_NOTES` identifiers in v8.9.91. |
| **#027** | **Persistent Viewer ID Reversion** | **Resolved**. Fixed logic in `ViewerService.handleTrackerPulse` (v8.9.88). |

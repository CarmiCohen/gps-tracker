# Testing & Validation Status

This document tracks pending unit tests, integration tests, and manual validation tasks.

## 🧪 Unit & Integration Test Backlog
| ID | Category | Task | Description |
| :--- | :--- | :--- | :--- |
| **#057** | **Unit Test** | **Adaptation Muzzle** | Verify `isAdaptationMuzzled` correctly suppresses `SentinelStatus.JUMP` during polling interval transitions (Critical for A15). |
| **#066** | **Unit Test** | **TrackerService Hilt** | Verify Hilt-based dependency injection for `TrackerService` after refactor (#058). |
| **#067** | **Unit Test** | **Settings Sanitization** | Add tests for `SettingsRepository` to ensure malformed IDs are reset to default without crashing (#042). |
| **#060** | **Integration** | **WorkManager Watchdog** | Verify `HiltWorkerFactory` configuration and ensure Workers revive the service after OS termination. |
| **#050** | **Integration** | **Migration Integrity** | Perform stress tests on table recreation migrations to ensure no silent data truncation occurs on large datasets. |
| **#056** | **Integration** | **Forensic Pipeline** | Verify end-to-end propagation of `vibrationRollingSum` and `tiltIdx` from Engine to Viewer HUD. |
| **#065** | **Integration** | **Forensic Consolidation** | Verify `ForensicLogUseCase` correctly standardizes pink logging across all modules (#061). |

## 🟡 Pending Manual Validation (Field Tests)
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#068** | **Logcat Audit** | Verify `getPackageName` noise is silenced on G990/A15 during map interaction (#005). |
| **#069** | **Identity Persistence** | Verify Viewer ID persists custom strings and defaults to "V" on fresh install. |
| **#070** | **UI Refresh Staleness** | Verify forensic fields (Prox Debounce/Rolling Vibe) respect the 15s staleness gate. |
| **#071** | **Forensic Stress Test** | Verify manual trigger of Jammer/Stall markers and HUD/Log reflection. |
| **R993** | **Notification Throttling** | Verify notification updates every 1s in foreground and 10s in background. |
| **#064** | **Diagnostics UI** | Verify "Permission Health Check" screen correctly identifies Xiaomi-specific states (#059). |
| **#063** | **Identity Conflict UX** | Verify UI feedback appears when `MainRepository` rejects a colliding ID (#039). |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required for `STABILITY GAP` logs. |
| **#053** | **Anchor Lock Breakout** | Physically move the device after a Hard-Lock and verify immediate breakout (Linked to Issue #062). |
| **#052** | **HUD Freshness Verification** | Verify that Tracker HUD/Viewer HUD line elements stay colorized when GPS is lost. |
| **#051** | **Binary Parity Verification** | Verify that a Viewer receiving a binary pulse correctly displays the `trackerState`. |
| **#046** | **State Sync Audit** | Verify that Tracker HUD and Viewer HUD transition between MOVING/PARKING simultaneously. |
| **#047** | **Speed Zeroing Verification** | Confirm Viewer HUD speed drops to 0.0 km/h immediately when Tracker GPS is lost. |
| **#043** | **Migration Verification** | Verify app starts without `IllegalStateException` on existing v53 databases. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |

---
*For manual testing procedures, refer to [DOCS/TESTS.md](../DOCS/TESTS.md).*

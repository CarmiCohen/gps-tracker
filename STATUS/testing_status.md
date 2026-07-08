# Testing & Validation Status

This document tracks pending unit tests, integration tests, and manual validation tasks.

## 🧪 Unit & Integration Test Backlog
| ID | Category | Task | Description |
| :--- | :--- | :--- | :--- |
| **#057** | **Unit Test** | **Adaptation Muzzle** | Verify `isAdaptationMuzzled` correctly suppresses `SentinelStatus.JUMP` during polling interval transitions (Critical for A15). |
| **#060** | **Integration** | **WorkManager Watchdog** | Verify `HiltWorkerFactory` configuration and ensure Workers revive the service after OS termination. |
| **#050** | **Integration** | **Migration Integrity** | Perform stress tests on table recreation migrations to ensure no silent data truncation occurs on large datasets. |
| **NEW** | **Unit Test** | **Settings Sanitization** | Add tests for `SettingsRepository` to ensure malformed IDs are reset to default without crashing. |

## 🟡 Pending Manual Validation (Field Tests)
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#056** | **Scale Bar Offset** | Verify "UNCERTAINTY" messages have sufficient vertical offset (~80dp) to not overlap the `osmdroid` scale bar in various screen densities. |
| **R993** | **Notification Throttling** | Verify notification updates every 1s in foreground and 10s in background. |
| **#031** | **Soak Test Monitoring** | Ongoing 24-hour stability test required for `STABILITY GAP` logs. |
| **#053** | **Anchor Lock Breakout** | Physically move the device after a Hard-Lock and verify immediate breakout (Linked to Issue #062). |
| **#052** | **HUD Freshness Verification** | Verify that Tracker HUD/Viewer HUD line elements stay colorized when GPS is lost. |
| **#051** | **Binary Parity Verification** | Verify that a Viewer receiving a binary pulse correctly displays the `trackerState`. |
| **#046** | **State Sync Audit** | Verify that Tracker HUD and Viewer HUD transition between MOVING/PARKING simultaneously. |
| **#047** | **Speed Zeroing Verification** | Confirm Viewer HUD speed drops to 0.0 km/h immediately when Tracker GPS is lost. |
| **#043** | **Migration Verification** | Verify app starts without `IllegalStateException` on existing v53 databases. |
| **#038** | **Adaptation Settling** | Monitor logcat for "Settling A15 Polling..." messages. |
| **#005** | **Log Spillage Hardening** | Confirm logcat is silent on G990/A155 regarding `getPackageName` spam. |
| **#033** | **Proto Precision Upgrade** | Verify existing `max_distance` and `max_accuracy` values are correctly interpreted in UI. |

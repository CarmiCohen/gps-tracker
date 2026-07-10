# Testing & Validation Status

This document tracks pending unit tests, integration tests, and manual validation tasks.

## 🧪 Unit & Integration Test Backlog
| ID | Category | Task | Description |
| :--- | :--- | :--- | :--- |
| **#072** | **Unit Test** | **Temporal Authority** | Verify `isGpsFresh` correctly handles up to 60s of clock skew using receipt-time calculation. |
| **#066** | **Unit Test** | **TrackerService Hilt** | Verify Hilt-based dependency injection for `TrackerService` after refactor (#058). |
| **#065** | **Integration** | **Forensic Consolidation** | Verify `ForensicLogUseCase` correctly standardizes pink logging across all modules (#061). |

## 🟡 Pending Manual Validation (Field Tests)
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#073** | **Peer Visibility (Issue C)** | Verify why Tracker shows "VWR" as red. Investigate pulse timestamp rejection in `RemoteHandler`. |
| **#074** | **Map Stabilization** | Verify tracker marker on viewer map does not jump to gray/raw locations during clock drift. |
| **#068** | **Logcat Audit** | Verify `getPackageName` noise is silenced on G990/A15 during map interaction (#005). |
| **#071** | **Forensic Stress Test** | Verify manual trigger of Jammer/Stall markers and HUD/Log reflection. |
| **#064** | **Diagnostics UI** | Verify "Permission Health Check" screen correctly identifies Xiaomi-specific states (#059). |
| **#053** | **Anchor Lock Breakout** | Physically move the device after a Hard-Lock and verify immediate breakout. |

## 🟢 Recently Verified (v9.3.8)
| ID | Task | Result |
| :--- | :--- | :--- |
| **#072** | **HUD Skew Fix** | Verified: HUD elements (Speed, State, Accuracies) remain green despite device clock drift. |
| **#047** | **Speed Zeroing** | Verified: Viewer HUD speed drops to 0.0 km/h immediately on Tracker GPS loss. |
| **#046** | **State Sync** | Verified: Tracker and Viewer HUDs transition between MOVING/PARKING simultaneously. |

---
*For manual testing procedures, refer to [DOCS/TESTS.md](../DOCS/TESTS.md).*

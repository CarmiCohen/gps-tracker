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
| **#074** | **Map Stabilization** | Verify tracker marker on viewer map does not jump to gray/raw locations during clock drift. |
| **#071** | **Forensic Stress Test** | Verify manual trigger of Jammer/Stall markers and HUD/Log reflection. |
| **#053** | **Anchor Lock Breakout** | Physically move the device after a Hard-Lock and verify immediate breakout. |

## 🔵 Ready for Verification
*No tasks currently pending verification.*

## 🟢 Recently Verified (v9.3.11)
| ID | Task | Result |
| :--- | :--- | :--- |
| **#064** | **Diagnostics UI** | **Verified**: UI correctly reflects live permission changes and hardware adaptations for Xiaomi/Samsung. |
| **#068** | **Logcat Audit** | **Verified**: Logcat spillage for `getPackageName` is silenced on Samsung devices via identifier caching. |

## 🟢 Recently Verified (v9.3.9)
| ID | Task | Result |
| :--- | :--- | :--- |
| **#073** | **Peer Visibility (Issue C)** | **Verified**: Tracker "VWR" badge now correctly turns green upon receipt of signaling pulses. |

## 🟢 Recently Verified (v9.3.8)
| ID | Task | Result |
| :--- | :--- | :--- |
| **#072** | **HUD Skew Fix** | Verified: HUD elements (Speed, State, Accuracies) remain green despite device clock drift. |
| **#047** | **Speed Zeroing** | Verified: Viewer HUD speed drops to 0.0 km/h immediately on Tracker GPS loss. |
| **#046** | **State Sync** | Verified: Tracker and Viewer HUDs transition between MOVING/PARKING simultaneously. |

---
*For manual testing procedures, refer to [DOCS/TESTS.md](../DOCS/TESTS.md). (v9.3.11: Added Section 5 for Diagnostics)*

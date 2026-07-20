# Testing & Validation Status

This document tracks pending unit tests, integration tests, and manual validation tasks.

## 🧪 Unit & Integration Test Backlog
| ID | Category | Task | Description |
| :--- | :--- | :--- | :--- |
| | | | *Backlog currently empty.* |

## 🟡 Pending Manual Validation (Field Tests)
| ID | Task | Verification Requirement |
| :--- | :--- | :--- |
| **#113** | **R405c Verification** | **New**. Perform long-term field testing on Samsung A15 hardware (SM-A155F) to confirm the Accelerometer fallback pulse prevents OS-level eviction when the Step Detector fails to register. |
| **#072** | **Map Stabilization** | Verify tracker marker on viewer map does not jump to gray/raw locations during clock drift. |
| **#071** | **Forensic Stress Test** | Verify manual trigger of Jammer/Stall markers and HUD/Log reflection. |
| **#062** | **Anchor Breakout (#053)** | Physically move the device after a Hard-Lock and verify immediate breakout with the new displacement-weighted monitor. |

## 🔵 Ready for Verification
| ID | Task | Result |
| :--- | :--- | :--- |
| **#108** | **Startup Recovery Race** | Verify that MaintenanceWorker does not trigger recovery during the staggered startup delay on vJuly.20.06. |
| **#080** | **Lift Detection Parity** | Verify `MainAlarmLogic` correctly uses `trackerBaroAltEma` delta for lift violations instead of raw altitude. |

## 🟢 Recently Verified (v9.3.15)
| ID | Task | Result |
| :--- | :--- | :--- |
| **#077** | **Type Safety Hardening** | **Verified**: Systematic audit and elimination of redundant `toDouble()`/`toFloat()` conversions. Standardized `AppSensorManager` with pre-allocated `DoubleArray` buffers. |

---
*For manual testing procedures, refer to [DOCS/TESTS.md](../DOCS/TESTS.md).*

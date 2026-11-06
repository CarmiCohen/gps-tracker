# Test Results - Nov6.1 (Real HUD Values)

## Nov6.1 Summary
Executed Nov 6, 2026 on the A15 (`R58X40GV2AR`, API 36), with Sensor and Monitor on the same phone through the production relay. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **145/145** | `:core-shared` 46, `:core-network` 16, `:core-ui` 13, `:app-monitor` unit 20, `:app-monitor` on A15 15, `:app-sensor` on A15 35. The A15 runs came before the version number change; no code changed after them. |
| **122.1 Local HUD line** | 🟢 **PASSED** | Local line `B:100%⚡\|T:29°C\|A:±77.6m\|AGE:23s`, then `AGE:38s`. `dumpsys battery`: level 100, status 5 (full), 29.1 to 29.3°C. |
| **122.2 Battery reading** | 🟢 **PASSED** | Automated; the Sensor's own HUD also showed the real `B:100%⚡\|T:29°C` on the A15. |
| **122.3 Unknown values** | 🟡 **AUTOMATED, PARTLY LIVE** | Local speed and state showed `--` with a network fix. A missing fix and missing telemetry were not observed live. |
| **122.4 Old fix** | 🟡 **AUTOMATED ONLY** | The Monitor was stationary throughout. |
| **Sensor line** | 🟢 **PASSED** | `STATIONARY 0.1 km/h`, `A:±11.2m`, `AGE:0s`, against GPS accuracy of about 11m. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor and Monitor processes: 0 Davey over 500ms, 0 StackLog, 0 fatal. |

**Run history:** a foreground run was stopped by the tool timeout. Running both instrumented modules in one Gradle call then failed with `Starting 0 tests` and install errors, before any test ran. Each module passed when run alone.

---

# Nov6.0 Results (Forensic Entry Integrity & Chapter Review)

## Nov6.0 Summary
Executed Nov 6, 2026 on the A15 (`R58X40GV2AR`, API 36). The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **130/130** | `:core-shared` 46, `:core-network` 16, `:core-ui` 5, `:app-monitor` unit 13, `:app-monitor` on A15 15, `:app-sensor` on A15 35. |
| **121.1 to 121.4 Entry integrity** | 🟢 **PASSED (automated)** | Interrupted states are simulated in the file. A deliberate break of the code failed 8 tests. Physical power loss was not run. |
| **121.5 Upgrade** | 🟢 **PASSED** | Full version 2 file upgraded in place on the A15: 10000 entries kept, numbering continued from 638988. |
| **121.6 Older Android** | 🟡 **AUTOMATED ONLY** | Compiled class checked; not run below API 35 (ISSUE-NOV6.0-O26). |
| **121.7 Process kill** | 🟢 **PASSED** | Three `kill -9` during 10Hz writing; restarted by itself each time; no damaged entries. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor process: 0 Davey over 500ms, 0 StackLog, 0 fatal. |
| **120.7 Backfill** | 🟡 **LOCAL RELAY ONLY** | Still not verified on production (ISSUE-NOV5.2-O12). |

---

# Nov5.2 Results (Chapter Triage & Runtime Resilience)

## Nov5.2 Summary
Executed Nov 5, 2026 on the A15 (`R58X40GV2AR`), running both Sensor and Monitor on the production relay unless noted. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **118/118** | `:core-shared` 34, `:core-network` 16, `:core-ui` 5, `:app-monitor` unit 13, `:app-monitor` on A15 15, `:app-sensor` on A15 35. |
| **54.1, 65.1, 120.1 Crash recovery** | 🟢 **PASSED** | Restarted in under 10s; sequence continued (518 to 600). |
| **70.1 Monitor crash** | 🟢 **PASSED** | Restarted and reconnected. |
| **16.1 Network loss** | 🟢 **PASSED** | 40s outage; reconnected in about 7s. |
| **23.1, 120.2 Doze** | 🟢 **PASSED** | 12m47s forced deep idle; wake lock held, telemetry continued every 10s. |
| **49.1 Charging rate** | 🟢 **PASSED** | 100ms charging, 10000ms on battery. |
| **73.1 Permission revoke** | 🟢 **PASSED** | Sensor stopped cleanly, no crash loop. |
| **0.4 Back button** | 🟢 **PASSED** | Service kept running. |
| **14.1, 42.1 Rotation** | 🟢 **PASSED** | No overlap in landscape. |
| **41.1 Dark mode** | 🟢 **PASSED** | App stayed up; legibility not separately measured. |
| **32.1, 120.8 Largest font** | 🟢 **PASSED** | Failed first; speed and labels now visible at 200%. |
| **38.1, 120.5 Bad relay URL** | 🟢 **PASSED** | Could not be saved; no crash. |
| **0.8, 120.9 ID isolation** | 🟢 **PASSED** | Failed first; no values carry over to a new pairing. |
| **5.2 Trail persistence** | 🟢 **PASSED** | Trail present immediately after force stop. |
| **113, 120.6 Menu** | 🟢 **PASSED** | All eight actions positioned on screen. |
| **118.8 Relay hot switch** | 🟢 **PASSED** | Sensor switched while connected in under 40ms. |
| **120.7 Backfill** | 🟡 **LOCAL RELAY ONLY** | 1 request, 7 chunks through local v6.044. Production relay is still v6.043. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`: 0 Davey over 500ms, 0 StackLog, 0 fatal on both processes. |
| **20.1 Reboot** | 🟢 **PASSED** | Sensor restarted by itself after boot, within about 3 minutes; sequence continued (about 2646 to 3054). |

**Run history:** Run 1 passed; font and backfill failures found live, fixed; Run 2 passed; isolation failure found live, fixed; Run 3 passed on the final code.

---

# Nov5.1 Results (Default Relay Correction & Production Verification)

## Nov5.1 Summary
Executed Nov 5, 2026 on the final build, with Sensor on A15 and Monitor on S21FE through the production relay `https://gps-survival-relay.onrender.com` (v6.043).

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **103/103** | `:core-shared` 27, `:core-network` 15, `:core-ui` 5, `:app-monitor` unit 9, `:app-monitor` on A15 12, `:app-sensor` on A15 35. |
| **119.1 Single default relay** | 🟢 **PASSED** | Automated by `RelayConfigTest`; both apps show the new default. |
| **119.2 Production relay link** | 🟡 **PARTIAL** | `SRV` green on both phones, and they reconnected about 4s after the relay restart. Recovery from an idle cold start over 20s was not observed (ISSUE-NOV5.1-O9). |
| **119.3 Production telemetry** | 🟢 **PASSED** | `TRK` and `VWR` active, `B:79%` matching the A15, live `AGE`, sensor marker at the real position. |
| **118.8 Relay hot switch** | 🟢 **PASSED** | S21FE moved from the old URL to the new one without an app restart; the timeouts stopped. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | Zero Davey, StackLog, or fatal entries from either app process, and no relay timeouts after the URL change. |

The first Nov5.1 live attempt showed `TRK` and `VWR` red because the deployed relay predated the apps' protocol. Publishing relay v6.043 resolved it. Section 3 below still applies: manual physical chapters were not executed in Nov5.1 either.

---

# Nov5.0 Results (HUD Direction, Real-Position Centering & Telemetry Integrity)

Executed Nov 5, 2026. Sensor on A15 (`R58X40GV2AR`), Monitor on S21FE (`R5CRC14PG4F`), both on system language `he-IL`.

## 1. Automated Suite (Final Code, Run 5)
All instrumented tests were pinned to the A15. Unit results cover debug and release variants where the module has both.

| Suite | Result |
| :--- | :--- |
| `:core-shared` unit (including `ForensicBufferStressTest`, `KinematicCalibrationTest`, `AlarmStealthTest`) | 🟢 24/24 |
| `:core-network` unit | 🟢 15/15 |
| `:core-ui` unit | 🟢 5/5 |
| `:app-monitor` unit | 🟢 9/9 |
| `:app-monitor` instrumented on A15 | 🟢 12/12 |
| `:app-sensor` instrumented on A15 (including `SensingEngineStressTest`, `ForensicStressAuditTest`, `HardeningAuditTest`, `PipelineSoakTest`, `ProductionReadinessAuditTest`) | 🟢 35/35 |
| **Total** | **🟢 100/100** |

`:app-sensor` has no unit tests.

### Run History
| Run | Result | Remediation |
| :--- | :--- | :--- |
| Pre-run | 🔴 Monitor instrumented suite did not compile | Added `viewerStatusFlow` to `FakeSignalingClient` |
| 1 | 🔴 `compactHudIsShorterThanStandardHud` failed | The test harness lacked the app theme; wrapped it in `RevolutionTheme`, threshold unchanged |
| 2 | 🟢 Passed | Live test then exposed zeroed telemetry; fixed |
| 3 | 🟢 Passed | Live test then exposed the `(0,0)` trail line; fixed |
| 4 | 🟢 Passed | Live test then exposed the ignored relay URL change; fixed |
| 5 | 🟢 Passed | Final code |

## 2. Two-Device Live Run (Sensor A15, Monitor S21FE)
**Constraint:** the configured relay did not respond because the apps used the wrong host (ISSUE-NOV5.0-O1, corrected in Nov5.1), so this run used the same `relay-server/index.js` on the workstation through `adb reverse`. Real internet relay traffic was not tested. The live run used the build before the final `connect()` change (Run 4 code); that change has automated coverage only.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **118.1 HUD left-to-right** | 🟢 **PASSED** | Both phones, `he-IL`: `INT` left of `WDG`, row 2 in correct order. |
| **118.2 Real-position centering** | 🟢 **PASSED** | Monitor on the real region and not California; sensor marker at the real position. |
| **118.3 Compact HUD** | 🟢 **PASSED** | Four lines in 202px on S21FE, no clipping. |
| **118.5 VWR presence** | 🟢 **PASSED** | Sensor `VWR` active with the Monitor connected. |
| **118.6 Telemetry integrity** | 🟢 **PASSED** | Relay carried real sequence and coordinates; Monitor showed `B:79%`, matching the A15, with a live `AGE`. |
| **118.7 No-fix filtering** | 🟢 **PASSED** | The line to `(0,0)` is gone. |
| **118.8 Relay hot switch** | 🟡 **AUTOMATED ONLY** | Unit test passes; not verified on hardware. |
| **106 / 116 LEDs** | 🟢 **PASSED** | `SRV`, `GPS`, `TRK`, `VWR`, `DAT`, `WDG` active with the link up; `SRV` and `TRK` red during the relay outage. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | 75s soak on both phones plus Run 5 logs: zero Davey and zero StackLog from the app processes. Lines seen came from other apps. No `FATAL`, ANR, `SQLiteDatabaseLocked`, or `NaNG`. |
| **Event log** | 🟢 **PASSED** | Sensor live event log and Monitor logcat showed service start and relay connection. |

## 3. Not Executed
The reciprocal simulation did not run, because the S21FE was connected and the two-device procedure applies instead.

**`TEST_PROCEDURE.md`:** chapters beyond those listed above were not individually executed as manual steps. Some logic is covered by the automated suites, but these need physical action or setup that was not performed, including GNSS shielding (5), stress and thermal triggers (6), sirens and alarms (8, 9, 26, 34), power and reboot (15, 20), network switching (16), multiple viewers (19, 40), TalkBack, font scaling, and orientation (31, 32, 42), sunlight and sensors (37, 43, 44), and long soaks (53, 93).

**`TEST_FORENSIC_STRESS_PROTOCOL.md`:** sections 1 to 5 are manual procedures and were not executed on the device UI. Related automated suites passed, but they do not replace these steps.

**Chapter 0:** 0.2, 0.4, 0.5, 0.7, 0.8, and 0.9 were not individually verified. Under 0.6, only the `VWR` badge was verified, not geofence sync.

---
*Verified on physical devices R58X40GV2AR (A15) and R5CRC14PG4F (S21FE) - Baseline Nov5.1*

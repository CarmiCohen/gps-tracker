# Chapter Triage - Nov7.21

Status of every chapter in `TEST_PROCEDURE.md`, `OPERATIONAL_FORENSIC_AUDIT.md`, and `TEST_FORENSIC_STRESS_PROTOCOL.md`, based on the running code and on tests executed for Nov5.2 on the A15. The A chapters were reviewed for Nov6.0 by comparing each chapter with its named test and with the production code (ISSUE-NOV5.4-O17).

| Code | Meaning |
| :--- | :--- |
| **L** | Verified live on hardware in Nov5.1 or Nov5.2 |
| **A** | Covered by a passing automated test of the real behavior |
| **T** | No automated test covers the claim; a test is possible but does not exist yet |
| **H** | Needs a person or physical action; not executed |
| **S** | Needs the S21FE or a second viewer; not executed in Nov5.2 |
| **D** | The feature is not wired into the running apps, so it cannot pass; decision needed |
| **N** | Obsolete since the app split; retirement proposed |

**D does not mean the code is absent.** In several chapters logic exists and has unit tests, such as `SentinelLogic`, but nothing in the running apps calls it. The D findings come from reading the code.

**Summary:** 66 of 130 active chapters are verified at least in part (L or A). The rest are waiting on a test, a person, a second device, or a decision on an unwired feature.

**Retired chapters:** N chapters are moved to `RETIRED_CHAPTERS.md`, leaving 111 active chapters at Nov6.0, including Chapter 121. Chapter 122 makes 112 in Nov6.1, Chapter 123 makes 113 in Nov6.2, Chapter 124 makes 114 in Nov6.3, Chapter 125 makes 115 in Nov6.4, Chapter 126 makes 116 in Nov7.0, Chapter 127 makes 117 in Nov7.1, Chapter 128 makes 118 in Nov7.2, Chapter 129 makes 119 in Nov7.3, Chapter 130 makes 120 in Nov7.5, Chapter 131 makes 121 in Nov7.6, Chapter 132 makes 122 in Nov7.7, Chapter 133 makes 123 in Nov7.9, Chapter 134 makes 124 in Nov7.10, Chapter 135 makes 125 in Nov7.11, Chapter 136 makes 126 in Nov7.12, Chapter 137 makes 127 in Nov7.15, Chapter 138 makes 128 in Nov7.17, Chapter 139 makes 129 in Nov7.19, and Chapter 140 makes 130 in Nov7.21. Corrected in Nov7.15: the Nov7.10 and Nov7.11 entries said 120 and 121 because chapters 131 to 133 had not been counted; the count now matches `TEST_PROCEDURE.md` (ISSUE-NOV7.15-O40). Items 8.3, 26.2, and 26.3 were retired in Nov7.3; chapters 8 and 26 stay active.

**A chapter review:** the earlier count of 48 included seven chapters whose tests do not prove the chapter's claim: 53, 66, 67, 68, 83, 88, and 93. None of the 48 were retired, so 48 minus 7 gives 41. ISSUE-NOV6.0-O20 then added chapters 66 and 121, giving 43. ISSUE-NOV5.0-O2 added chapter 122, giving 44. ISSUE-NOV5.2-O14 added chapter 123 and verified chapter 50, giving 46. ISSUE-NOV5.0-O5 added chapter 124, giving 47. ISSUE-NOV6.3-O32 added chapter 125, giving 48. ISSUE-NOV5.0-O6 added chapter 126, giving 49. ISSUE-NOV5.2-O15 added chapter 127, giving 50. ISSUE-NOV6.1-O27 added chapter 128, giving 51. ISSUE-NOV5.2-O11 group 1 added chapter 129 and verified chapters 8 and 27, giving 54. ISSUE-NOV5.2-O11 group 1 slice 2 added chapter 130, giving 55. Recounted in Nov7.15 (ISSUE-NOV7.15-O40), because the Nov7.6 to Nov7.12 additions were not all recorded here: chapter 131 gave 56 in Nov7.6, as the dashboard shows; chapter 132 gives 57, chapter 133 gives 58, chapter 134 gives 59, and chapter 87 moving from D to A in Nov7.10 gives 60; ISSUE-NOV6.0-O24 added chapter 135, giving 61; ISSUE-NOV6.1-O28 added chapter 136, giving 62; ISSUE-NOV6.4-O33 added chapter 137, giving 63. ISSUE-NOV6.4-O34 added chapter 138, giving 64. ISSUE-NOV7.17-O42 added chapter 139, giving 65. ISSUE-NOV7.19-O43 added chapter 140, giving 66.

## Chapter 0 - Operational Forensic Audit
| Item | Code | Notes |
| :--- | :--- | :--- |
| 0.1 Logcat anomalies | L | Now checked by `TESTS/device_audit.cmd` |
| 0.2 Pulses and pruning | D | No stay-alive pulse; the trail pruning worker is scheduled since Nov6.2 |
| 0.3 Tracker stealth | L | Sensor HUD shows no alarm or siren indicators |
| 0.4 Back button | L | Service keeps running after Back |
| 0.5 UI alignment | D | No scale bar on the live map; version is on the first screen only |
| 0.6 Peer sync | L / D | `VWR` verified; geofence sync does not exist |
| 0.7 Setup closure | L | `CLOSE (X)` works |
| 0.8 ID isolation | L | Failed, fixed, and verified: no values from another device |
| 0.9 Telemetry continuity | D | No ping, noise, or lux telemetry |

## TEST_PROCEDURE.md
| Chapters | Code | Notes |
| :--- | :--- | :--- |
| 1 Deployment | L / D | Install and version verified; `PLATINUM` branding does not exist |
| 2 Setup | N / L | 2.1 and 2.2 retired; 2.3 target ID survived `install -r` live (chapter 24). `IdentityPersistenceTest` only re-reads the same manager, and editing the self ID in the UI then reopening was not exercised |
| 3 Tracker mode | L / H / D | HUD verified; 3.2 shake is physical; 3.3 has no JNI, lux, or tilt |
| 4 Viewer sync | L | 4.3 to 4.6 verified; 4.2 verified in Nov7.3: alarms appear on the Monitor only (chapter 129.7), and `SensorStealthGuardTest` keeps the Sensor silent |
| 5 GNSS and persistence | D / L | 5.1 no `SIGNAL LOSS` state; 5.2 verified |
| 6 Stress and heat | D / A | Injection buttons do not exist; 6.2 10s interval automated, but no `Cooling Mode` text exists |
| 7 Map and geofencing | D | No anchor or geofence on the live path |
| 8 Alarm authority | L / A | 8.1 automated by `SensorStealthGuardTest`; 8.2 verified on the A15 in chapter 129.7. 8.3 retired in Nov7.3 |
| 27 System health alarms | L / A | 27.1 and 27.2 verified on the A15 in chapter 129.7 with a simulated 15% battery and 47°C, and automated in `MonitorAlarmsTest` |
| 9, 26, 28, 29 Alarms | D | Sensor violations are never sent or received; 26.2 and 26.3 retired in Nov7.3; 29 relay and signal alarms are the next slice |
| 10, 47, 80, 94 JNI | N | Retired: no native code in the project |
| 11, 21, 61, 62, 98 Motion logic | D | `SentinelLogic` is tested but unused by the Sensor |
| 12, 46, 69 Workers | D | 12 has no pressure control; 46 periodic work cannot be expedited; 69 no `MaintenanceWorker` exists |
| 50 Periodic cleanup | L | Trail worker ran 15 minutes apart on the A15; it is the only scheduled worker |
| 13 Remote commands | D | No command channel |
| 14 UI performance | L | Rotation verified |
| 15 Power tamper | D | Not implemented |
| 16 Network resilience | L | About 7s reconnection after data loss |
| 17, 35, 79 Export | D | No export |
| 18 Integrity | D | No diagnostics screen |
| 19, 40 Multiple viewers | S | Needs extra devices |
| 20 Reboot | L | Sensor restarted by itself within a few minutes; sequence continued |
| 22 Alias | D | No alias field |
| 23 Doze | L | 12m47s in forced deep idle: wake lock held, telemetry continued |
| 24 Migration | L | `install -r` kept the pairing and data; Room version unchanged, so no schema migration was exercised |
| 25 Map follow | L / D / A | 25.1 verified; 25.2 ghost mode not wired; 25.3 automated |
| 30, 33, 96, 99, 108 Map tools | D | Only in `OsmMap`, which has no callers |
| 31 TalkBack | H | Needs a person with TalkBack |
| 32 Font scaling | L | Failed, fixed, and verified |
| 34 Siren overlay | D | Alarm screen only reachable through unused FCM path |
| 36, 37, 43, 44, 64 Other sensors | D | Only accelerometer and gravity are registered |
| 38 Relay URL input | L | Malformed URLs rejected |
| 39 Buffer rollover | A | `ForensicBufferSchemaTest`, `ForensicStressAuditTest` |
| 41 Dark mode | L | App stays usable; HUD colors are fixed |
| 42 Landscape | L | No overlap |
| 45, 77 Thermal | A / D | 45 interval hysteresis automated by `ThermalHysteresisTest`, but no `Cooling Mode` text exists; 77 cooling window not wired |
| 48, 63 Motion stability | D | Activity recognition and auto-tune not used |
| 49 Charging rate | L | 100ms while charging is by design; verified live, and no automated test asserts it |
| 51, 52 Permission recovery | D | No microphone or overlay use |
| 53 Long run | H | 1 hour run not executed; `PipelineSoakTest` does not check polylines |
| 93 Soak | T | `PipelineSoakTest` is 2000 events, not a 24 hour cycle, and runs `SentinelLogic`, which the Sensor does not use. Zero loss cannot hold as written: 10000 entries last 16.7 minutes at 10Hz |
| 54 Sticky restart | L | Restarted in under 10s |
| 55 Backoff | L | Retries observed |
| 56 Protocol mismatch | D | No version check |
| 57 Large payload | L | Backfill arrived in 7 chunks through a local v6.044 relay |
| 58 Deduplication | A | `SignalingQueueTest.testPartitionDeduplicationAndBufferFlag` only. `LogRepository` does not exist, and duplicates stored by the Monitor are untested (ISSUE-NOV5.2-O16) |
| 59, 60, 76 Display conditions | H | High contrast, sunlight, Samsung deep sleep |
| 65 LMK sequence | L | Sequence continues after a crash |
| 66 Crash during write | A | `ForensicEntryIntegrityTest` opens files left in the state an interrupted write or power loss leaves; damaged entries are skipped (ISSUE-NOV6.0-O20). States are simulated; physical power loss was not run |
| 67 Burst latency | T | `ForensicBurstAuditTest` bounds the average at 5ms, not each write, and uses `ForensicBuffer`, not a WAL database |
| 68 Scale | T | `TrailCapTest` uses 1200 rows and no timing; the chapter requires 50,000 points in under 2s |
| 70 Process death | L | Monitor recovered |
| 71, 78, 84, 89, 92 Identity and roles | N | Retired: split apps and fixed default IDs |
| 87 Activity routing | A | Verified: overlay flags, dialog states, and follow mode use `rememberSaveable` in MonitorActivity and AdminActivity, persisting across rotation and configuration changes (ISSUE-NOV6.0-O22) |
| 72, 74 Log encryption and stripping | D | `CryptoUtils` is unused |
| 73 Foreground revoke | L | Engine stops cleanly |
| 75 UI rate | A | Verified: UiRateTest and HudRateTest cover 2Hz rate-limiting for trail and HUD flows (ISSUE-NOV6.0-O21) |
| 81, 82, 85, 86, 90, 91, 95, 97 Internals | D | Described behavior not found in the running apps |
| 83 Stall watchdog | T | `WatchdogRecoveryTest` only checks a fresh heartbeat; no stall is forced. The loop runs on `Dispatchers.Default` with a 10s minimum threshold, so the chapter's 5s main-thread block cannot trigger recovery |
| 88 Hydration | D | No staggered hydration runs: `hydrationLevel` is only reached through `OsmMap`, which has no callers. `HydrationStaggeringAuditTest` only times `delay(100)` |
| 100 SOT parity | D | Cannot pass while D chapters remain |
| 101, 102, 103 IDs and session | L / A | Verified |
| 104 Partition tolerance | A | `SignalingResilienceTest` |
| 105, 106, 107 Relay and LEDs | L | Verified |
| 109 Alarm setup UI | H / D | Since Nov7.3 the Monitor switches for the four chapter 129 alarms, Global Mute, and the Siren & Audio settings drive real alarms, and `TEST SIREN AUDIO` plays on the Monitor. The other switches still drive nothing, and the Sensor's test button does nothing (ISSUE-NOV7.3-O38) |
| 110 Device hardening | D | `DeviceProfileManager` is never called |
| 111, 112 Tools and clean setup | D | Screens open, but most actions are empty. The three Monitor `CLEAN` actions only unpair, after a confirmation since Nov7.1 (ISSUE-NOV7.1-O35) |
| 113, 114 Menu | L / D | All buttons visible; Events and Graphs do nothing |
| 115, 116, 118, 119 | L | Verified |
| 117 | N | Retired: Nov4.44 procedure record |
| 120 Nov5.2 resilience | L / A | See `test_results.md` |
| 121 Forensic entry integrity | L / A | 121.1 to 121.6 automated; 121.5 and 121.7 verified on the A15. 121.6 was not run below API 35 (ISSUE-NOV6.0-O26) |
| 122 Real HUD values | L / A | 122.1 to 122.4 automated; 122.1 verified on the A15 against `dumpsys`. 122.3 and 122.4 were not observed live |
| 123 Trail storage bound | L / A | 123.1 to 123.4 automated; 123.4 and 123.5 verified on the A15: 9,799 of 59,799 rows pruned by the installed app |
| 124 Real relay status text | L / A | 124.1 automated; 124.2 verified on the A15 with the dialog open. The `SRV` LED colour was not read during 124.2 |
| 125 Backfill gap recovery | L / A | 125.1 to 125.6 automated; 125.7 verified on the A15 through a private relay, with one hole. Production relay and two phones not run |
| 126 Real version strings | L / A | 126.1 and 126.2 automated; 126.3 verified on the A15 through the production relay. The `--` case has automated proof only |
| 127 Unpair confirmation | L / A | 127.1 and 127.2 automated; 127.3 verified on the A15 through the production relay |
| 128 Real Sensor HUD values | L / A | 128.1 to 128.3 automated; 128.4 verified on the A15 through the production relay against `dumpsys location`. The return to `--` after the service stops has automated proof only, because nothing on the device can stop the service (ISSUE-NOV7.2-O36) |
| 129 Monitor alarms and siren | L / A | 129.1 to 129.6 automated; 129.7 verified on the A15 through the production relay at low volume. The full-screen start over a locked screen and the maximum-volume path on hardware were not run |
| 130 Communication alarms | A | 130.1 to 130.3 automated by `MonitorAlarmsTest` |
| 131 30s Siren Auto-Stop | A | 131.1 and 131.2 automated by `MonitorAlarmEngine` |
| 132 False MOVING & Violations | A | 132.1 to 132.3 automated by `SentinelLogicTest` and `MonitorAlarmsTest` |
| 133 HUD flow 2Hz rate limit | A | 133.1 automated by `UiRateTest` and `HudRateTest` |
| 134 Screen state preservation | A | 134.1 automated by UI state persistence tests |
| 135 SignalingQueue thread safety | A | 135.1 and 135.2 automated by `SignalingQueueTest` |
| 136 Coarse Network Fix Filtering | A | 136.1 and 136.2 automated by `MonitorLocationSourceTest` |
| 137 Backfill stops on send failure | A | 137.1 to 137.3 automated by `BackfillResponderTest`; 137.4, the Monitor asking again after a real network cut, was not run on hardware |
| 138 Real Sensor service status | L / A | 138.1 and 138.2 automated by `SensorServiceStatusTest`; 138.3 verified on the A15 through the production relay (Stopped / Disconnected without the service, Running / Connected after start) |
| 139 No false alarms at start | L / A | 139.1 to 139.3 automated by `MonitorAlarmsTest`, `MovementCheckTest`, and `SentinelLogicTest`; 139.4 verified on the A15 through the production relay (Monitor cold start and a start of both apps with no alarm, siren, or violation; a still phone with no violation for 2 minutes) |
| 140 The Sensor reports a real GPS loss | L / A | 140.1 to 140.3 automated by `SignalingQueueTest`, `SignalingBatchTest`, `FixFreshnessTest`, and `MovementCheckTest`; 140.4 verified on the A15 through the production relay (`NO_FIX` 15 s after the last fix, 126 contiguous `NO_FIX` rows stored by the Monitor, no violation after recovery). The Monitor's 30 s loss alarm on `NO_FIX` was not sounded live; it is covered by `MonitorAlarmsTest` |

## TEST_FORENSIC_STRESS_PROTOCOL.md
| Section | Code | Notes |
| :--- | :--- | :--- |
| 1 High-frequency IMU | D | No burst control; no JNI layer |
| 2 Database pressure | D | No pressure test control or export; trail pruning is scheduled since Nov6.2 |
| 3 Forensic injection | D | No injection button or markers |
| 4 UI saturation | L / D | Rotation verified; no 100Hz stream control |
| 5 GNSS zombie | H | Needs shielding and a clock change |

# Chapter Triage - Nov5.2

Status of every chapter in `TEST_PROCEDURE.md`, `OPERATIONAL_FORENSIC_AUDIT.md`, and `TEST_FORENSIC_STRESS_PROTOCOL.md`, based on the running code and on tests executed for Nov5.2 on the A15.

| Code | Meaning |
| :--- | :--- |
| **L** | Verified live on hardware in Nov5.1 or Nov5.2 |
| **A** | Covered by a passing automated test of the real behavior |
| **H** | Needs a person or physical action; not executed |
| **S** | Needs the S21FE or a second viewer; not executed in Nov5.2 |
| **D** | The feature is not wired into the running apps, so it cannot pass; decision needed |
| **N** | Obsolete since the app split; retirement proposed |

**D does not mean the code is absent.** In several chapters logic exists and has unit tests, such as `SentinelLogic`, but nothing in the running apps calls it. The D findings come from reading the code.

**Summary:** 48 of 120 chapters are verified at least in part (L or A). The rest are waiting on a person, a second device, or a decision on an unwired feature.

## Chapter 0 - Operational Forensic Audit
| Item | Code | Notes |
| :--- | :--- | :--- |
| 0.1 Logcat anomalies | L | Now checked by `TESTS/device_audit.cmd` |
| 0.2 Pulses and pruning | D | No stay-alive pulse; pruning worker never scheduled |
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
| 2 Setup | N / A | "Tap Tracker" predates the split; ID persistence automated |
| 3 Tracker mode | L / H / D | HUD verified; 3.2 shake is physical; 3.3 has no JNI, lux, or tilt |
| 4 Viewer sync | L / D | 4.3 to 4.6 verified; 4.2 alarms not wired |
| 5 GNSS and persistence | D / L | 5.1 no `SIGNAL LOSS` state; 5.2 verified |
| 6 Stress and heat | D / A | Injection buttons do not exist; thermal logic automated |
| 7 Map and geofencing | D | No anchor or geofence on the live path |
| 8, 9, 26, 27, 28, 29 Alarms | D | No siren playback; violations are never sent or received |
| 10, 47, 80, 94 JNI | N | No native code in the project |
| 11, 21, 61, 62, 98 Motion logic | D | `SentinelLogic` is tested but unused by the Sensor |
| 12, 46, 50, 69 Workers | D | WorkManager is never scheduled |
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
| 45, 77 Thermal | A / D | Hysteresis automated; 77 cooling window not wired |
| 48, 63 Motion stability | D | Activity recognition and auto-tune not used |
| 49 Charging rate | L / A | 100ms while charging is by design |
| 51, 52 Permission recovery | D | No microphone or overlay use |
| 53, 93 Long runs | H / A | 1 to 24 hour soak not executed; `PipelineSoakTest` is short |
| 54 Sticky restart | L | Restarted in under 10s |
| 55 Backoff | L | Retries observed |
| 56 Protocol mismatch | D | No version check |
| 57 Large payload | L | Backfill arrived in 7 chunks through a local v6.044 relay |
| 58 Deduplication | A | `SignalingQueueTest`, `StartupIntegrityTest` |
| 59, 60, 76 Display conditions | H | High contrast, sunlight, Samsung deep sleep |
| 65 LMK sequence | L | Sequence continues after a crash |
| 66, 67 Database atomicity | A | Recovery and burst audits |
| 68 Scale | A | Display capped at 1000 points |
| 70 Process death | L | Monitor recovered |
| 71, 78, 84, 87, 89, 92 Identity and roles | N / A | Split apps and fixed default IDs; identity logic automated |
| 72, 74 Log encryption and stripping | D | `CryptoUtils` is unused |
| 73 Foreground revoke | L | Engine stops cleanly |
| 75 UI rate | A | `UiRateTest` |
| 81, 82, 85, 86, 90, 91, 95, 97 Internals | D | Described behavior not found in the running apps |
| 83 Stall watchdog | A | `WatchdogRecoveryTest`; a 5s main-thread block was not executed |
| 88 Hydration | A | `HydrationStaggeringAuditTest`, `StartupIntegrityTest` |
| 100 SOT parity | D | Cannot pass while D chapters remain |
| 101, 102, 103 IDs and session | L / A | Verified |
| 104 Partition tolerance | A | `SignalingResilienceTest` |
| 105, 106, 107 Relay and LEDs | L | Verified |
| 109 Alarm setup UI | H / D | Settings exist but drive no alarms |
| 110 Device hardening | D | `DeviceProfileManager` is never called |
| 111, 112 Tools and clean setup | D | Screens open, but most actions are empty |
| 113, 114 Menu | L / D | All buttons visible; Events and Graphs do nothing |
| 115, 116, 118, 119 | L | Verified |
| 117 | N | Nov4.44 procedure record |
| 120 Nov5.2 resilience | L / A | See `test_results.md` |

## TEST_FORENSIC_STRESS_PROTOCOL.md
| Section | Code | Notes |
| :--- | :--- | :--- |
| 1 High-frequency IMU | D | No burst control; no JNI layer |
| 2 Database pressure | D | No pressure test control, export, or scheduled pruning |
| 3 Forensic injection | D | No injection button or markers |
| 4 UI saturation | L / D | Rotation verified; no 100Hz stream control |
| 5 GNSS zombie | H | Needs shielding and a clock change |

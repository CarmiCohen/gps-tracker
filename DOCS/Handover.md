git add .
git commit -m "Nov5.2 - CHAPTER TRIAGE & RUNTIME RESILIENCE

- ISSUE-NOV5.0-O4 TRIAGE: Classified every chapter in TESTS/CHAPTER_TRIAGE.md; 48 of 120 are verified at least in part. Added TESTS/device_audit.cmd, a real per-process Davey and StackLog audit. QA is now a measured count.
- ISSUE-NOV5.2-O11 FINDING: Recorded that many documented features are not wired into the running apps; a decision is needed on each.
- ISSUE-NOV5.0-O3: Explained the telemetry interval: 100ms is the designed charging rate, 10000ms when stationary on battery.
- GAP-REVOL71.0-G1 SEQUENCE: Sensor sequence numbers continue from the forensic buffer after a crash or reboot.
- GAP-REVOL71.0-G2 WAKE LOCK: The watchdog renews the 10-minute wake lock.
- GAP-REVOL71.0-G3 TRAIL: The map receives at most the newest 1000 points, at no more than 2Hz.
- GAP-REVOL71.0-G4 RELAY URL: Malformed URLs are rejected in both apps; a stored malformed URL falls back to the default.
- GAP-REVOL71.0-G5 MENU: All eight Monitor menu actions stay visible.
- GAP-REVOL71.0-G6 FONT: HUD and map button text is capped at 130% so readings stay visible.
- GAP-REVOL71.0-G7 BACKFILL: Backfill responses carry a routable id. Relay v6.044 is verified locally but not deployed (ISSUE-NOV5.2-O12).
- GAP-REVOL71.0-G8 ISOLATION: The Monitor HUD shows telemetry only from the paired device.
- TESTS: Added SequenceContinuityTest, UiRateTest, TrailCapTest, PairedTelemetryTest, and new RelayConfigTest, SignalingBatchTest and HudLayoutAuditTest cases. 118 of 118 automated tests pass, including 50 on the A15.
- LIVE: A15 pass on the production relay covering crash, reboot, network loss, Doze, rotation, font scaling, isolation, and URL validation.
- DOCS: Added Chapter 120, Rules 1.355 and 1.356, Chapter 5.63, open gaps ISSUE-NOV5.2-O11 to O16, and baseline STATUS/BASELINE/Nov5.2/.
- VERSION: Nov5.2, versionCode 33 (monitor) and 35 (sensor)."
git tag Nov5.2
git push origin main --tags

# Project Handover - Version Nov5.2 (Chapter Triage & Runtime Resilience)

## ▶️ Resumption State (Read First)
- **Last commit**: `1168760` (Nov5.1), tag `Nov5.1`, branch `main`, level with `origin/main`.
- **Nov5.2 is complete but NOT committed or tagged.** The Git block at the top of this file is ready to run. `chat_session.md` holds the full resumption log and is ignored by Git.
- **Relay**: production runs v6.043 (`9a20990`). `relay-server/index.js` lines 51, 123, 124 hold uncommitted v6.044 backfill routing. The automated push was blocked as a production deploy, so the user must run it. Stage `index.js` only.
- **Phones**: A15 (`R58X40GV2AR`) runs the Nov5.2 Sensor, service active, default relay, original settings restored; the test copy of the Monitor was removed. S21FE (`R5CRC14PG4F`) is a personal phone, still on Nov5.1, and must not be touched until the user allows it.
- **Pending user actions**: run the Git block; decide on the relay push; optionally add the shell permission rule to `.claude/settings.local.json`, which Claude cannot write itself; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O12 Relay v6.044 undeployed | Medium | `relay-server/index.js:123-124` |
| O2 Hardcoded local HUD | Medium | `MonitorViewModel.kt:120` |
| O4 Remaining H and S chapters | Medium | `TESTS/CHAPTER_TRIAGE.md` |
| O13 False MOVING | Medium | `MonitorViewModel.kt:164` |
| O14 Trail storage unbounded | Medium | `TrailPruningWorker.kt` never scheduled |
| O5 False relay status text | Low | `MonitorActivity.kt:473` |
| O6 Stale version strings | Low | `SensorService.kt:107`, `MonitorActivity.kt:232,234`, `AdminActivity.kt:253,255` |
| O7 Zero-position rows stored | Low | `MapCenterResolver.kt:6` filters display only |
| O9 Cold start exceeds 20s timeout | Low | `SocketioSignalingClient.kt` `setTimeout(20000)` |
| O10 Synchronous relay cache writes | Low | `relay-server/index.js` `saveCache` |
| O15 Gear unpairs immediately | Low | `MonitorMenuBar`, `MonitorActivity.kt:563` |
| O16 Sequence restarts after fresh install | Low | `SensorService.kt:116`, `MonitorDatabase.kt` unique index |

### Nov5.2 Fix Locations
`SensorService.kt:116` sequence seed, `:364` wake lock renewal; `ForensicBuffer.kt:147`; `RelayConfig.kt:13,24`; `UiRate.kt`; `CappedFontScale.kt:15`; `MonitorDatabase.kt:10`; `MonitorViewModel.kt:24`; `MonitorActivity.kt:563`; `SocketioSignalingClient.kt:350`.

## 🟡 Status: VERIFIED ON A15, RELAY v6.044 NOT DEPLOYED (Nov5.2)

Version Nov5.2 delivers these changes:
1. **Triage**: Every chapter has a status in `TESTS/CHAPTER_TRIAGE.md`; 48 of 120 are verified at least in part.
2. **Major finding**: Many documented features are not wired into the running apps (ISSUE-NOV5.2-O11).
3. **Resilience**: Sequence continuity, wake lock renewal, 1000-point trail cap, 2Hz trail updates.
4. **Safety and display**: Relay URL validation, visible menu actions, font cap, paired-device isolation.
5. **Backfill**: Client fixed; relay v6.044 proven on a local relay but not deployed.

**Resumption Token**: NOV5.2-DECISIONS-PENDING

---

## 🎯 Next Resumption Focus: Decide unwired features (ISSUE-NOV5.2-O11), then deploy relay v6.044 (ISSUE-NOV5.2-O12)

1. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule.
2. Commit and push `relay-server/index.js` (v6.044). The automated push was blocked as a production deploy, so it needs the user. Afterwards, confirm backfill on the production relay.
3. Run the H chapters with a person, and the S chapters when the S21FE is free.

### 🛡️ Implementation Status (Nov5.2)
- **Version Increment**: ✅ **VERIFIED** (`Nov5.2`, `versionCode` 33 monitor, 35 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (118/118; instrumented tests pinned to A15).
- **A15 Live Pass**: ✅ **PASSED** on the production relay, except backfill, which was verified locally.
- **S21FE**: ❌ **NOT USED**; it still runs Nov5.1.
- **Physical Chapters & Stress Protocol**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
- `app-sensor/.../SensorService.kt`, `core-shared/.../ForensicBuffer.kt`: sequence continuity, wake lock renewal.
- `core-shared/.../RelayConfig.kt`, `UiRate.kt`: URL validation, 2Hz limit.
- `core-ui/.../CappedFontScale.kt`, `HudStatusBar.kt`: font cap.
- `app-monitor/.../MonitorActivity.kt`, `MonitorViewModel.kt`, `MonitorDatabase.kt`: `MonitorMenuBar`, paired telemetry, trail cap.
- `core-network/.../SocketioSignalingClient.kt`: routable backfill responses.
- `relay-server/index.js`: v6.044 routing, uncommitted; separate repository.
- `TESTS/CHAPTER_TRIAGE.md`, `TESTS/device_audit.cmd`: triage and audit tool.

---

## ⚠️ Operational Notes
- `TESTS/device_audit.cmd <serial> <package>` is the real Step 2 audit; `ProductionReadinessAuditTest` only checks math.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- Instrumented runs reinstall the Sensor, so its service must be started again from `START SENSOR SERVICE`.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov5.2)
1. **Automated Suite**: 118/118 on the final code.
2. **Hardware**: Crash, reboot, network loss, Doze, rotation, font, and isolation verified on the A15; 0 Davey, StackLog, or fatal entries from either app process.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov5.2/`; tag `Nov5.2` is applied by the Git block above.

**Handover complete. System state locked at Nov5.2.**

```
git commit -m "Nov8.12 - GEOFENCING IMPLEMENTATION (ISSUE-NOV5.2-O11 GROUP 2), committed during Nov8.13 (ISSUE-NOV8.13-O53)

- Staged snapshot of Nov8.12, which was never committed: SensorService geofence check against a home anchor, AlarmType.GEOFENCE_BREACH in MonitorAlarms controlled by AlertSettings.distance, GeofenceBreachAlarmTest, tracking documents, baseline STATUS/BASELINE/Nov8.12/.
- Known at commit time (fixed in Nov8.13): the home anchor was the Sensor's own first fix (fixed 100 m, reset on each start) instead of the user's Home Points; gradle.properties was not bumped; test chapter 155 was cited but not written."
git tag -f Nov8.11 dd4be20
git tag -f Nov8.12
git add .
git commit -m "Nov8.13 - HOME POINTS GEOFENCE, MONITOR DEFINED AND SENSOR ENFORCED (ISSUE-NOV8.13-O51, O54, O53)

- ISSUE-NOV8.13-O51 (High): replaced the Nov8.12 self-anchored geofence. User decision 2026-11-08, option B (battery): the Monitor defines Home Points and one radius on the live map (HOME edit mode, radius slider 50-500 m, green squares with remove confirmation, radius circles, red OUT dots from GEOFENCE_BREACH trail rows, real Home Point count in Phone Setup) and only delivers them; the Sensor stores the newest revision and enforces it with SentinelLogic.evaluateHomeZone (any point, GPS accuracy hysteresis, no transition on the first fix after a start or change).
- core-shared: HomePoint, GeofenceConfig (withPoint, withoutPoint, withRadius, cleared, revisedAfter), GEOFENCE_BREACH_STATUS, SentinelLogic.evaluateHomeZone.
- core-network: SignalingClient.geofenceFlow and sendGeofence over the relay's existing settings_update / settings_relay event (GeofenceWire); relay unchanged (v6.044).
- app-sensor: SensorDataStore.geofence and saveGeofence (newest revision only); SensorService self-anchor (homeGeofence, HOME_ANCHOR, 100 m) removed, setupGeofenceListener added.
- app-monitor: MonitorDataStore.geofence and editGeofence; GeofenceSync.kt geofenceDeliveries used by MonitorSessionService (each edit, each time the tracker comes online); MonitorViewModel addHomePoint, removeHomePoint, setGeofenceRadius, clearHomePoints; MonitorActivity HOME editor, markers, HomePointEditor, osmdroid zoom buttons hidden; MapCenterResolver.geofenceBreaches; MonitorAlarms uses GEOFENCE_BREACH_STATUS.
- ISSUE-NOV8.13-O54 (Low): Clear Home Points removes the points only and no longer switches the distance, jammer, tilt, lift, and tamper alarms off (user decision 2026-11-08).
- ISSUE-NOV8.13-O53 (part): gradle.properties Nov8.13 / 64 (Nov8.12 never bumped), VersionSourceTest checks the format instead of pinned values, chapter 155 written; the Nov8.12 commit and the tags Nov8.11 and Nov8.12 repaired by this block.
- TESTS: new HomeZoneTest (7), GeofenceWireTest (3), GeofenceSyncTest (3); extended GeofenceBreachAlarmTest (telemetry path), CleanSetupActionsTest, VersionSourceTest; fakes updated (SignalingClientTest, BackfillContentionTest). 249 unit tests pass across all subprojects; assembleDebug and assembleRelease successful; ProductionReadinessAuditTest 3/3 on the A15; geofence add, radius, remove, and re-send verified on the A15 through the production relay. TEST_PROCEDURE chapters 155 and 156 (146 of 156 active), CHAPTER_TRIAGE 85 of 146.
- New open gaps: ISSUE-NOV8.13-O55 (Medium, Monitor first frame 705-783 ms, identical on the Nov8.11 build), ISSUE-NOV8.13-O56 (Low, every Sensor defaults to ID T), ISSUE-NOV8.13-O57 (Low, Gradle 9 deprecations under --warning-mode all). Work Queue row 8 corrected: O11 Groups 4 and 5 are not built. Ideas SIMP-NOV8.13-S1 and S2, SIMP-NOV5.0-S1 extended.
- DOCS: issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.394, 468 rules, chapter 5.100), TEST_PROCEDURE.md, CHAPTER_TRIAGE.md, MAP_FUNCTIONALITY_SPEC.md, 04_COMMUNICATION_PROTOCOL.md, ALARM_AND_SIREN_MECHANISM.md, SETUP_AND_SOUND_OPTIONS.md, DOCS/Handover.md, Handover.md, baseline STATUS/BASELINE/Nov8.13/.
- VERSION: Nov8.13, versionCode 64."
git tag Nov8.13
git push origin --force Nov8.11 Nov8.12
git push origin main --tags
```

# Project Handover - Version Nov8.13 (Home Points Geofence, Monitor Defined and Sensor Enforced)

## ▶️ Resumption State (Read First)
- **Version**: Nov8.13 is closed but **not committed**. Run the Git block above once, in order: it first commits the staged Nov8.12 snapshot alone (the index still holds exactly Nov8.12; the Nov8.13 work is unstaged), moves the tags Nov8.11 and Nov8.12 onto their own code, then commits and tags Nov8.13 and force-pushes only the two moved tags (ISSUE-NOV8.13-O53). `git log --oneline -3` afterwards should show Nov8.13, Nov8.12, Nov8.11, each with its own tag. Before this block: tag Nov8.12 was on `dd4be20` (the Nov8.11 commit) and tag Nov8.11 on `e45d8fd` (the Nov8.10 commit), locally and on `origin`.
- **Next work**: see `## 🎯 Next Resumption Focus` below.
- **User decisions (2026-11-08)**: ISSUE-NOV8.13-O51 option B: the Sensor enforces the geofence and the Monitor only edits and sends it, to save the Monitor's battery; in any phone combination the Monitor sends the Home Points to the Sensor. Clear Home Points removes the points only (ISSUE-NOV8.13-O54). Device tests on the A15 only, with the Monitor paired to the A15's own Sensor. Earlier decisions: ISSUE-NOV5.2-O11 groups 1 to 3 built, groups 4 (Microphone) and 5 (Lift) to be built, groups 6 to 8 not required; ISSUE-NOV6.0-O26 waits for a device below API 35 (no API 34 image download); `before splitting/gps-tracker` is a frozen archive, never edit it (ISSUE-NOV5.4-O19).
- **Relay**: v6.044 on production. No relay code changed in Nov6.4 to Nov8.13; the geofence uses the existing `settings_update` / `settings_relay` handler.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached; both apps on it are Nov8.13 debug builds, the Monitor is paired to `T`, the Sensor uses the default ID `T` (it was reinstalled by the instrumented run, so its buffer and sequence restarted). The Monitor's geofence is empty with radius 350 m (test residue; move the slider back to 100 m if wanted). S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: app logs use the tag prefix `REV_` (for example `REV_Geofence`, `REV_MonitorService`). Read logcat with `adb logcat -d adbd:S` (not `-s adbd:S`, which silences everything). `TESTS/device_audit.cmd` must run from `cmd`, one call per app; read its `davey_over_500ms` count, because `echo %errorlevel%` on the same line prints the old value. The Sensor admin screen starts as `System Idle`; one tap opens it and starts the service. Instrumented tests uninstall the Sensor; reinstall it afterwards with `:app-sensor:installDebug`. Run instrumented tests from PowerShell with `$env:ANDROID_SERIAL="R58X40GV2AR"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Groups 4 and 5 not built | High | `AlertSettings.acousticAlert`, `liftAlert`, `tiltAlert` have no Sensor code |
| O53 Nov8.12 commit and tags | Medium | Git block above (user action) |
| O55 Monitor first frame 705-783 ms | Medium | `MonitorActivity` start; Perfetto trace needed |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O4 Test coverage gap | Medium | H and S rows in `TESTS/CHAPTER_TRIAGE.md` |
| O56 Every Sensor defaults to ID `T` | Low | `ForensicIdentity.DEFAULT_TRACKER_ID` |
| O57 Gradle 9 deprecations | Low | build logic that mutates resolved configurations |
| O47 Partial or missing STATUS/BASELINE copies | Low | `STATUS/BASELINE/` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |

---

## 🟢 Status: PASSED (Nov8.13)

Version Nov8.13 delivers ISSUE-NOV8.13-O51 and ISSUE-NOV8.13-O54, and the version part of ISSUE-NOV8.13-O53:
1. **Root cause found (O51)**: the Nov8.12 Sensor set `homeGeofence` to its own first fix with a fixed 100 m radius after every service start, so "home" followed the tracker; the Monitor's Home Points were never filled, and the `ADD`/`DEL` tools exist only in the unused `MapComponents.kt` stack.
2. **Geofence, Monitor defined**: `HOME` in the live map's control column opens `HomePointEditor` (count, radius slider 50 to 500 m stored on release, `DONE`). A map tap adds a Home Point (green square plus radius circle); tapping one asks `Remove Home Point N?`. Stored as `GeofenceConfig` with a growing revision (`MonitorDataStore.editGeofence`, `GeofenceConfig.revisedAfter`). Breach rows show as red OUT dots (`geofenceBreaches`). Phone Setup shows the real Home Point count. osmdroid's own zoom buttons are hidden, because they covered the slider.
3. **Delivery**: `geofenceDeliveries(dataStore.geofence, signalingClient.viewerStatusFlow)` in `MonitorSessionService` sends on each edit and each time the tracker comes online, through `SignalingClient.sendGeofence` (`settings_update`, `type = geofence`, `GeofenceWire`). The relay forwards it as `settings_relay`; it keeps no copy. The Monitor does no per-fix geofence work.
4. **Geofence, Sensor enforced**: `SensorService.setupGeofenceListener` stores each received geofence when its revision is newer (`SensorDataStore.saveGeofence`) and enforces the stored one, also offline and after a restart. Each fix goes through `SentinelLogic.evaluateHomeZone`: home within the radius of any point; outside only when even the near edge of the GPS error circle is beyond every radius; state reset without a transition on a start or a change; `GEOFENCE_BREACH` (critical status plus `ViolationEvent`) only on leaving home. The self-anchor code is removed.
5. **Clear Home Points (O54)**: `MonitorViewModel.clearHomePoints` removes the points only; alarm settings and alarms are untouched. Full re-initialization also resets the geofence.
6. **Version (O53 part)**: `gradle.properties` Nov8.13 / 64; `VersionSourceTest` checks the format and a minimum code.
7. **Tests & build**: 249 unit tests, 0 failures, across `core-shared`, `core-network`, `app-monitor`, `app-sensor`, `core-ui` (new `HomeZoneTest` 7, `GeofenceWireTest` 3, `GeofenceSyncTest` 3; extended `GeofenceBreachAlarmTest`, `CleanSetupActionsTest`, `VersionSourceTest`; `TrackingDocumentIntegrityTest` 6/6 on the final documents). `assembleDebug` and `assembleRelease` successful with `--warning-mode all`; the Gradle 9 deprecations it prints predate Nov8.13 (O57). `ProductionReadinessAuditTest` 3/3 on the A15.
8. **On device (A15, production relay)**: with the Sensor on a temporary ID `A15GEO` and the Monitor paired to it: add (`REV_Geofence: Received 1 home points ... stored: true` 0.4 s after the tap), radius 350 m sent once on release, remove after confirmation (0 points), re-send of the same revision not stored again, no crash, no false breach. A real exit needs a moving tracker and is covered by `HomeZoneTest` only. Both IDs and the pairing were restored to `T` afterwards.
9. **Forensic audit (Completion step 2)**: Sensor 0 Davey, 0 StackLog, 0 fatal. Monitor 0 StackLog, 0 fatal, but **2 Davey frames over 500 ms on its first frame** (711/783 ms after install, 744/774 ms after a force stop). The Nov8.11 build, built in a temporary worktree and measured the same way, gave 705/768 and 748/782 ms, so Nov8.13 did not cause it; recorded as ISSUE-NOV8.13-O55.
10. **Documents**: SOT rule 1.394 (rule 1.393 marked superseded), 468 rules, chapter 5.100; `RESOLUTION_ARCHIVE.md`; `DOCS/issues.md` (Work Queue rows 42 to 47, row 8 corrected, open gaps O53, O55, O56, O57, ideas SIMP-NOV8.13-S1 and S2); `TESTS/TEST_PROCEDURE.md` chapters 155 and 156 (146 of 156 active), 147.1 and 153.1 updated; `TESTS/CHAPTER_TRIAGE.md` 85 of 146; `MAP_FUNCTIONALITY_SPEC.md`, `04_COMMUNICATION_PROTOCOL.md`, `ALARM_AND_SIREN_MECHANISM.md`, `SETUP_AND_SOUND_OPTIONS.md`, `DOCS/Handover.md`; baseline `STATUS/BASELINE/Nov8.13/` copied byte for byte from the final files.

**Audit line**: Nov8.13: [SOT Count: 968 (Rules: 468), Open: H:1, M:3, L:15, Ideas: H:0, M:3, L:15, Testing: PASSED (Sub-items: 331), QA: 85]

**Resumption Token**: NOV8.13-HOMEPOINTS-O51-NEXT

---

## 🎯 Next Resumption Focus
1. The user runs the Git block above (ISSUE-NOV8.13-O53).
2. ISSUE-NOV5.2-O11 Group 4 (Microphone) or Group 5 (Lift), High, both decided "Build" and not built; the user picks which first.
3. ISSUE-NOV8.13-O55 (Medium): Perfetto trace of the Monitor start on the A15.
4. ISSUE-NOV6.0-O26 stays blocked on a device below API 35.

---

## 📂 Key Implementation Files
- `core-shared/src/main/kotlin/com/jd/core/shared/Models.kt` (`HomePoint`, `GeofenceConfig`, `GEOFENCE_BREACH_STATUS`), `SentinelLogic.kt` (`evaluateHomeZone`).
- `core-network/src/main/kotlin/com/jd/core/network/SignalingClient.kt`, `SocketioSignalingClient.kt` (`sendGeofence`, `settings_relay`, `GeofenceWire`).
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorService.kt`, `SensorDataStore.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorActivity.kt` (`HOME`, `HomePointEditor`), `MonitorViewModel.kt`, `MonitorDataStore.kt`, `MonitorSessionService.kt`, `GeofenceSync.kt`, `MapCenterResolver.kt`, `MonitorAlarms.kt`.
- Tests: `core-shared/src/test/.../HomeZoneTest.kt`, `VersionSourceTest.kt`; `core-network/src/test/.../GeofenceWireTest.kt`; `app-monitor/src/test/.../GeofenceSyncTest.kt`, `GeofenceBreachAlarmTest.kt`, `CleanSetupActionsTest.kt`.
- `gradle.properties`, `TESTS/TEST_PROCEDURE.md`, `TESTS/CHAPTER_TRIAGE.md`, `DOCS/issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `Handover.md`.

**Handover complete. System state locked at Nov8.13.**

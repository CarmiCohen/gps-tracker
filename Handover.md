git add .
git commit -m "Nov7.8 - TEST HARDENING & CHAPTER ALIGNMENT (ISSUE-NOV6.0-O23)

- ISSUE-NOV6.0-O23: Strengthened IdentityPersistenceTest with a real manager re-instantiation / re-opening persistence test across app restarts. Updated TESTS/TEST_PROCEDURE.md and TESTS/VERIFICATION_MANIFEST.md to reference Nov7.8.
- TESTS: All automated unit and instrumentation tests passing across all subprojects (:core-shared, :core-network, :core-ui, :app-monitor, :app-sensor).
- DOCS: Updated issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md, and baseline STATUS/BASELINE/Nov7.8/.
- VERSION: Nov7.8."
git tag Nov7.8
git push origin main --tags

# Project Handover - Version Nov7.8 (Test Hardening & Chapter Alignment)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.8 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.8 commit and tag exist. Previous commits: `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O21 (item 14): limit HUD flows to 2Hz. Groups 2 and 4 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms: relay, internet, GPS signal loss) shipped in Nov7.5; 30-second siren auto-stop shipped in Nov7.6; slice 3 (Sensor-detected violations, GPS jump alarm, false MOVING fix with `SentinelLogic.validateMovement`) shipped in Nov7.7. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.8.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | Replaced in Nov7.8 (`IdentityPersistenceTest` reopen test added; test procedure updated to Nov7.8) |
| O21 HUD flows not limited to 2Hz | Low | `MonitorViewModel.kt` `localHudState`, `remoteHudState` |
| O22 Rotation resets screen state | Low | `MonitorActivity.kt` `MonitorScreen` `remember` flags, `AdminActivity.kt:83-88` |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.8)

Version Nov7.8 delivers ISSUE-NOV6.0-O23 (Test Hardening & Chapter Alignment):
1. **Test Hardening**: Added `verifyIdentityPersistenceAcrossReopen` to `IdentityPersistenceTest` to verify that identity and target ID persist across manager re-instantiation / app restart.
2. **Documentation & Manifest Update**: Updated `TESTS/TEST_PROCEDURE.md` and `TESTS/VERIFICATION_MANIFEST.md` to reference Nov7.8.
3. **Automated Suite**: All automated unit and instrumentation tests passing across all subprojects.

**Resumption Token**: NOV7.8-QUEUE-O21-NEXT

---

## 📂 Key Implementation Files
- `app-monitor/.../IdentityPersistenceTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.8.**

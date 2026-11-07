git add .
git commit -m "Nov7.15 - BACKFILL ANSWER STOPS ON SEND FAILURE (ISSUE-NOV6.4-O33)

- ISSUE-NOV6.4-O33: The Sensor no longer keeps sending backfill chunks after one fails. The chunk loop moved from SensorService.setupBackfillListener to the new answerBackfill in app-sensor BackfillResponder.kt (BACKFILL_CHUNK_SIZE 50). It stops at the first chunk sendBackfillResponse cannot send and never sends a later chunk or the completion chunk, so the Monitor no longer counts a range with missing rows as answered. The Monitor is unchanged: BackfillLedger releases the unfinished request after 30 s and the stored-trail gap scan asks again, with no restart. Added BackfillResponderTest (7 tests), the first JVM unit test in app-sensor.
- ISSUE-NOV7.15-O40 (new, partly fixed): tracking documents drifted during Nov7.6 to Nov7.12. Fixed: SOT header (Nov7.15, 448 rules), CHAPTER_TRIAGE and VERIFICATION_MANIFEST headers, triage active-chapter chain (127 active) and QA recount (63), resolved O13, O21, O22, O24, O28 removed from Open Gaps. ISSUE-NOV6.0-O23 reopened as partly done. Still open: missing dashboard entries Nov7.7 to Nov7.12 and Resolved Gaps Nov7.6 to Nov7.9.
- ISSUE-NOV7.15-O41 (new, open): the Sensor logged one Davey frame above 500 ms at start in 2 of 3 A15 audit runs (BOOT_COMPLETED on leaving stopped state starts SensorService during the first AdminActivity frame). Not caused by O33.
- TESTS: all JVM unit tests passing (111 Android-module, 46 core-shared); ProductionReadinessAuditTest 3/3 on the A15; device audit Monitor clean, Sensor see O41.
- DOCS: issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.373), TEST_PROCEDURE.md (Chapter 137), CHAPTER_TRIAGE.md, VERIFICATION_MANIFEST.md, Handover.md, baseline STATUS/BASELINE/Nov7.15/.
- VERSION: Nov7.15, versionCode 48 (monitor) and 50 (sensor). There was no Nov7.13 or Nov7.14 release."
git tag Nov7.15
git push origin main --tags

# Project Handover - Version Nov7.15 (Backfill Answer Stops On Send Failure)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.15 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.15 commit and tag exist. Previous commits: `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, or Nov7.14 release (the user chose Nov7.15 directly after Nov7.12).
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O26 (item 18), still blocked on a device below API 35 or permission to download an API 34 emulator image. If the user prefers other work, take the first item marked `Nothing`: ISSUE-NOV6.4-O34 (item 26, fixed text in the Sensor Setup dialog). New since Nov7.15: ISSUE-NOV7.15-O41 (item 33, Sensor start-up Davey) and ISSUE-NOV7.15-O40 (item 32, missing dashboard history). Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.15.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached, running the Nov7.15 debug builds of both apps with location and notification permissions granted. S21FE (`R5CRC14PG4F`) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `TESTS/VERIFICATION_MANIFEST.md` |
| O41 Sensor start-up Davey | Low | `BootReceiver.onReceive`, `SensorService.onCreate` on the main thread at first `AdminActivity` frame |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard (Nov7.7 to Nov7.12) and Resolved Gaps (Nov7.6 to Nov7.9) |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟡 Status: PASSED WITH A RECORDED AUDIT FINDING (Nov7.15)

Version Nov7.15 delivers ISSUE-NOV6.4-O33 (Backfill Answer Stops On Send Failure):
1. **Fix**: `app-sensor/.../BackfillResponder.kt` adds `answerBackfill(requestId, updates, send)`. It sends chunks of 50 rows, stops at the first chunk that cannot be sent, and never sends a later chunk or the `isComplete` chunk after a failure; an empty range gets one empty completion chunk. `SensorService.setupBackfillListener` calls it with `signalingClient::sendBackfillResponse` and logs a warning when it returns false. The old inline loop is removed.
2. **Monitor path (unchanged, verified by reading)**: `MonitorRepository.observeBackfill` stores each chunk and marks the request answered only on `isComplete`. Without it, `BackfillLedger` drops the request after `REQUEST_TIMEOUT_MS` (30 s) with no chunks, and the next gap scan, on a sequence jump or every 30 s of telemetry, asks again for the rows still missing.
3. **Tests**: `app-sensor/src/test/.../BackfillResponderTest.kt`, 7 tests, the first JVM unit tests in `:app-sensor`. `gradlew test assembleDebug assembleRelease --warning-mode all`: BUILD SUCCESSFUL, 111/111 Android-module and 46/46 `core-shared` unit tests passing, with no new warnings (only the existing Gradle deprecations and kapt option warning). There is no `platinum` variant.
4. **Device (A15 only)**: `ProductionReadinessAuditTest` 3/3. `TESTS/device_audit.cmd`, 3 runs of about 2 minutes: Monitor 0 Davey, StackLog, or fatal entries in all; Sensor 0 StackLog and fatal entries, but one Davey over 500 ms in 2 runs, the first frame at start (ISSUE-NOV7.15-O41, not caused by O33). Chapter 137.4, a real network cut during a backfill, was not run.
5. **Documents**: SOT rule 1.373 (R-ID 144) and header corrected to 448 rules; `RESOLUTION_ARCHIVE.md`; Chapter 137 in `TESTS/TEST_PROCEDURE.md` (127 of 137 active); `TESTS/CHAPTER_TRIAGE.md` chain and QA recount (63 of 127); `TESTS/VERIFICATION_MANIFEST.md` header; `DOCS/issues.md` queue, Open Gaps cleanup, O40, O41, SIMP-NOV7.15-S1, dashboard; baseline `STATUS/BASELINE/Nov7.15/`.

**Audit line**: Nov7.15: [SOT Count: 948 (Rules: 448), Open: H:1, M:3, L:15, Ideas: H:0, M:3, L:11, Testing: PASSED (Sub-items: 160), QA: 63]

**Resumption Token**: NOV7.15-BACKFILL-O26-NEXT

---

## 📂 Key Implementation Files
- `app-sensor/src/main/kotlin/com/jd/app/sensor/BackfillResponder.kt` (new), `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorService.kt` (`setupBackfillListener`), `app-sensor/src/test/kotlin/com/jd/app/sensor/BackfillResponderTest.kt` (new).
- `app-monitor/build.gradle.kts`, `app-sensor/build.gradle.kts` (versions).
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.15.**

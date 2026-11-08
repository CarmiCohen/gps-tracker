git add .
git commit -m "Nov7.38 - LOCAL TRAIL STORAGE AND PRUNING (ISSUE-NOV6.2-O29)

- ISSUE-NOV6.2-O29: Built local trail feature on the Monitor. Monitor stores its own movement path (isLocalUser = true) in local Room database (TrailDao) upon receiving location fixes from MonitorLocationSource. MonitorViewModel exposes localTrail and MonitorActivity renders independent polylines for remote tracker trail (Blue) and local viewer trail (Cyan) on the osmdroid map canvas. Activated automatic 48-hour TTL pruning via TrailDao.pruneLocalTrail and TrailPruningWorker.
- TESTS: All JVM unit tests passing across all subprojects (66 in app-monitor), assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 21 done), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.384, 458 rules), TEST_PROCEDURE.md (Chapter 148, 138 of 148 active), Handover.md, baseline STATUS/BASELINE/Nov7.38/.
- VERSION: Nov7.38."
git tag Nov7.38
git push origin main --tags

# Project Handover - Version Nov7.38 (Local Trail Storage and Pruning)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.38 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.38 commit and tag exist. Previous commits: `Nov7.36`, `Nov7.34`, `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.2-O30 (item 22, Low, decided: retain historical data of unpaired trackers)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34. Independent clean setup maintenance actions shipped in Nov7.36 (ISSUE-NOV7.1-O35). Local trail feature shipped in Nov7.38 (ISSUE-NOV6.2-O29). Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen. Nov7.19: O42 chosen. Nov7.21: O43 chosen. Nov7.24: O45 chosen. Nov7.26: O44 chosen. Nov7.28: O38 chosen. Nov7.30: O37 chosen. Nov7.32: O39 chosen. Nov7.34: O36 chosen. Nov7.36: O35 chosen. Nov7.38: O29 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.38.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `VERIFICATION_MANIFEST.md` |
| O41 Start-up Davey | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard and Resolved Gaps |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |

---

## 🟢 Status: PASSED (Nov7.38)

Version Nov7.38 delivers ISSUE-NOV6.2-O29:
1. **Local Trail Storage & Pruning** (`MonitorRepository`, `MonitorViewModel`, `MonitorActivity`, `TrailDao`, `TrailPruningWorker`):
   - Monitor records and stores its own movement path (`isLocalUser = true`) in the local Room database (`TrailDao`) upon receiving location fixes from `MonitorLocationSource`.
   - `MonitorViewModel` exposes `localTrail` StateFlow.
   - `MonitorActivity` renders independent polylines for remote tracker trail (Blue) and local viewer trail (Cyan) on the osmdroid map canvas.
   - Automatic 48-hour TTL pruning via `TrailDao.pruneLocalTrail` and `TrailPruningWorker`.
2. **Tests & Build**: All JVM unit tests passing across all subprojects (66 in `app-monitor`), `assembleDebug --warning-mode all` build clean.
3. **Documents**: SOT rule 1.384 (R-ID 155), 458 rules; `RESOLUTION_ARCHIVE.md`; Chapter 148 in `TESTS/TEST_PROCEDURE.md` (138 of 148 active); `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.38/`.

**Audit line**: Nov7.38: [SOT Count: 958 (Rules: 458), Open: H:1, M:2, L:12, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 286), QA: 71]

**Resumption Token**: NOV7.38-LOCALTRAIL-O29-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.2-O30 (Work Queue item 22, Low, decided: retain historical data of unpaired trackers).

---

## 📂 Key Implementation Files
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorRepository.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorViewModel.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorActivity.kt`.
- `app-monitor/src/test/kotlin/com/jd/app/monitor/LocalTrailTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.38.**

```
git add .
git commit -m "Nov11.0 - THE LATE VIEWER MEASURED (ISSUE-NOV10.7-O73)

- O73 diagnosed, stays open: the S21FE delays the Monitor's connection in the background (alerts up to 3.4 s, rows up to 18 s); the A15 gets alerts in 0.3 s. Fix: O86.
- New ArrivalLagMeter: each Monitor logs alert-row lag and a 10 s lag summary (SOT 1.418, chapter 179).
- New: O87 relay sends backfill replies to the whole room. O86 raised to next item.
- Index row Nov10.9-moreIdeas filled (688e5e1).
- TESTS: run_tests (unit, assembleDebug, assembleRelease); A15 ProductionReadinessAuditTest 3/3, release cold-start audit clean.
- VERSION: Nov11.0, versionCode 89."
git tag Nov11.0
git push origin main --tags
```

# Project Handover - Version Nov11.0 (The Late Viewer Measured)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov11.0^{commit}`.
- **User decisions (2026-11-11)**: O73 closed for now as a diagnosis; O86 (alarms by high-priority push) is the next item, as O73's fix. When the effort level should change, stop and ask the user before continuing. `//` means "continue". Ask before each use of the S21FE.
- **Phones**: A15 (`R58X40GV2AR`): Nov11.0 debug builds, Sensor `T` running (`mic=LISTENING`), Monitor `V` paired to `T`, session running. S21FE (`R5CRC14PG4F`): release Monitor Nov11.0 (data kept), session running, battery optimisation unchanged (on).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (new: reading the `REV_Signaling` lag lines); `TESTS/TEST_GUIDELINES.md` section 4. Instrumented tests can run from an installed test APK with `adb shell am instrument -w -e class <class> com.jd.app.sensor.test/com.jd.app.sensor.HiltTestRunner`, which keeps app data.

## 🟢 Status: PASSED (Nov11.0)
- ISSUE-NOV10.7-O73 diagnosed (stays open, fix by O86): Samsung delays the background S21FE Monitor; service, freezer and battery optimisation ruled out. New ISSUE-NOV11.0-O87. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1 test assembleDebug assembleRelease` passed (345 debug-variant unit tests); A15 `ProductionReadinessAuditTest` 3/3; release cold-start audit clean for both apps.

**Audit line**: Nov11.0: [SOT Count: 992 (Rules: 492), Open: H:0, M:8, L:14, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 338), QA: 114]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 71, ISSUE-NOV10.8-O86: design alarms by high-priority Firebase push (Firebase project, relay credentials, an always-awake relay O9); likely several versions (`/effort high`). O87 can go with its relay change.
3. Then O76, O82 and O81, O83, O84 (three versions), O85, O80.

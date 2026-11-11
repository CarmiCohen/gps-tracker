```
git add .
git commit -m "Nov11.1-push - ALARMS ALSO BY PUSH (ISSUE-NOV10.8-O86, ISSUE-NOV11.0-O87)

- O86 first version, stays open: the Sensor's alarm rows also reach the Monitor as high-priority Firebase pushes; one alarm engine, one raise per alarm (SOT 1.419, chapter 180).
- O87 resolved: the relay sends backfill replies only to the Monitor that asked (relay v6.045, gps-survival-relay fc2fbd4).
- New: O88 residual push risks. O73 stays open until the S21FE is tested. SIMP-NOV7.3-S2 closed.
- Index row Nov11.0 filled (1d0c901).
- TESTS: run_tests (594 unit tests, assembleDebug, assembleRelease); A15 ProductionReadinessAuditTest 3/3, release cold-start audit clean, push 0.5 to 1.2 s.
- VERSION: Nov11.1-push, versionCode 90."
git tag Nov11.1-push
git push origin main --tags
```

# Project Handover - Version Nov11.1-push (Alarms Also By Push)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov11.1-push^{commit}`. It commits `app-monitor/google-services.json` too (needed by the build).
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), already committed and pushed (`fc2fbd4`, v6.045) and live on Render with `push: on`. Nothing to do there.
- **User decisions (2026-11-11)**: Claude pushes the relay repo; ask before each use of the S21FE; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov11.1-push debug builds, Sensor `T` running (`mic=LISTENING`), Monitor `V` paired to `T`, session running. S21FE (`R5CRC14PG4F`): not connected in this session; still release Monitor Nov11.0, which has no push.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24, google-services 4.4.0 (app-monitor). Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (new: Alarm push; never force-stop the Monitor before a push test); `TESTS/TEST_GUIDELINES.md` section 4. The scripts are in `TESTS\` (`TESTS\run_tests.ps1`).

## 🟢 Status: PASSED (Nov11.1-push)
- ISSUE-NOV10.8-O86 first version done (stays open for the later versions); ISSUE-NOV11.0-O87 resolved; new ISSUE-NOV11.1-O88. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `TESTS\run_tests.ps1 test assembleDebug assembleRelease` passed (594 tests); relay `npm test` 9/9; A15 `ProductionReadinessAuditTest` 3/3; release cold-start audit clean for both apps.
- A15: three noise alarms arrived by push in 469 to 1213 ms (relay about 300 ms), one raise each.
- Not run: the S21FE (not connected), so ISSUE-NOV10.7-O73 is not confirmed fixed; a push starting a dead session (Android restarted the killed session within 1 s).

**Audit line**: Nov11.1-push: [SOT Count: 993 (Rules: 493), Open: H:0, M:8, L:14, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 343), QA: 115]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. When the S21FE is available (ask first): install the Nov11.1-push Monitor on it and run chapter 180.5 to confirm O73 (`/effort low`; high only if it fails).
3. Item 71, ISSUE-NOV10.8-O86 next versions: relay-raised pushes (Sensor offline, with O83), then a Monitor without a permanent connection (`/effort high`).
4. Otherwise O76, O82 and O81, O83, O84 (three versions), O85, O80.

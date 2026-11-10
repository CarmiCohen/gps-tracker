```
git add .
git commit -m "Nov10.8 - BACK CLIMBS, NEVER CLOSES (ISSUE-NOV10.8-O78, O77, O79)

- ISSUE-NOV10.8-O78: Back on any setup page returns one level up; at the top it sends the app to the background.
- ISSUE-NOV10.8-O77: Phone Setup Close X stays on screen; new bottom Close button.
- ISSUE-NOV10.8-O79: duplicate Settings wheel removed from the SENSOR ADMIN title row.
- New: ISSUE-NOV10.8-O76 (Sensor Admin top bar buttons), SIMP-NOV10.8-S1; SOT 1.417; chapter 178.
- Index row Nov10.7 filled (ea0a8a6).
- TESTS: 500 unit tests, debug+release builds; A15: SetupNavigationTest 3/3, ProductionReadinessAuditTest 3/3, audit 0/0/0 both apps.
- VERSION: Nov10.8, versionCode 87."
git tag Nov10.8
git push origin main --tags
```

# Project Handover - Version Nov10.8 (Back Climbs, Never Closes)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.8^{commit}`.
- **User decisions (2026-11-10)**: items 53 (O61) and 62 (O71) are very low priority. `//` from the user means "continue". Ask before each use of the S21FE.
- **Phones**: A15 (`R58X40GV2AR`): Nov10.8 debug builds; data wiped by the instrumented runs and restored: Sensor `T` running (location all the time, microphone, notification access), Monitor `V` paired to `T`, no Home Points. The Sensor's sequence restarted at 0 (O16: the S21FE Monitor holds older rows). S21FE (`R5CRC14PG4F`): untouched, release Monitor Nov10.3 paired to `T`.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (new: instrumented runs wipe the Monitor too, with the restore commands); `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov10.8)
- ISSUE-NOV10.8-O78, O77, O79 resolved; O74's Back half fixed. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 500 unit tests, debug and release builds; A15: chapter 178 passed, `ProductionReadinessAuditTest` 3/3, release cold-start audit 0/0/0 for both apps.

**Audit line**: Nov10.8: [SOT Count: 991 (Rules: 491), Open: H:0, M:3, L:11, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 336), QA: 113]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 63, ISSUE-NOV10.7-O73: find where the second viewer's 11 to 17 s delay arises (`/effort high`; needs the S21FE with the user's permission).
3. Item 64, ISSUE-NOV10.8-O76: decide the Sensor Admin top bar buttons with the user; SIMP-NOV10.8-S1 would also close O74 (`/effort medium`).

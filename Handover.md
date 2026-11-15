```
git add .
git commit -m "Nov15.4 - THE EVENTS LOG RETURNS (ISSUE-NOV10.8-O84 part 1)

- O84 part 1: both apps keep their events 7 days in a file (core-shared EventLog); menu Events button opens the list (1 h/6 h/24 h/7 d, Details, Export by share sheet, Clear with confirmation).
- The Sensor sends its newest 50 events as a sensor_events settings message (relay unchanged); the Monitor lists them marked S, once each.
- Instrumented tests use their own event files. New gap O98. Index row Nov15.3 filled (a55ad8f).
- TESTS: unit 734; instrumented Sensor 43/43, Monitor 55/55; ProductionReadinessAuditTest 3/3; release audits clean; A15 live check.
- VERSION: Nov15.4, versionCode 103."
git tag Nov15.4
git push origin main --tags
```

# Project Handover - Version Nov15.4 (The Events Log Returns)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov15.4^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov15.4 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov15.4 debug Sensor `T` and Monitor `V` paired again after the Gradle instrumented runs uninstalled both (permissions re-granted, battery exemption restored for both, detection settings at defaults rev 0, home points empty, earlier events lost); Sensor test APK installed; airplane mode off; `KEEP_WHEN_UNUSED` missing (real). S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (prefer `am instrument` with installed test APKs: it keeps the apps' data); chapter 190.5 for the live Events check; Monitor instrumented runs in airplane mode (O90).

## 🟢 Status: PASSED (Nov15.4)
- ISSUE-NOV10.8-O84 part 1 (Events) done; parts 2 (detailed Info) and 3 (Graphs) open; new Low gap ISSUE-NOV15.4-O98; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 734 passed; `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 190.5 live (two fixes found there: events re-sent on a Monitor's settings message, Events screen left to right); instrumented Sensor 43/43, Monitor 55/55; `ProductionReadinessAuditTest` 3/3; release cold-start audits clean.
- Not run: Clear on the phone (kept the log; unit-tested); stress protocol (no change in that area); chapters marked H or S; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov15.4: [SOT Count: 1003 (Rules: 503), Open: H:0, M:5, L:19, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 375), QA: 125]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O84 part 2: the detailed Info; agree its contents first (`/effort high`).
4. O84 part 3: the Graphs, with a 7-day sample store (`/effort high`).
5. Then O85, O80; O98, O86, O94 to O97 when convenient.

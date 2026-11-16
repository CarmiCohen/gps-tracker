```
git add .
git commit -m "Nov16.6 docs - two-phone test (ISSUE-NOV15.6-O100 part 6)

- Two-phone outage 125.7 (A15 Sensor, S21FE release Monitor Nov16.6): clean; recorded in the archive and Handover.
- Index row Nov16.6 filled (4a7fa39). No tag: Nov16.6 is already tagged."
git push origin main
```

# Project Handover - Version Nov16.6 (Pruned Holes Are Not Gaps)

## ▶️ Resumption State (Read First)
- **Git**: Nov16.6 is committed, tagged and pushed (`4a7fa39`); the block above commits the two-phone notes after it, without a tag. The index row Nov16.6 is already filled.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.6 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1` is in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.6 debug Sensor and Monitor, paired. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112, user decision 2026-11-16), paired to `T`, Global Mute on; the stray debug Sensor was removed.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov16.6)
- ISSUE-NOV15.6-O100 part 6 fixed; parts 1, 3 and 4 open; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `TESTS/run_tests.ps1 test assembleDebug assembleRelease --rerun-tasks`: PASSED (849), no new warning.
- A15: `BackfillGapRecoveryTest` 10/10 (new test for 125.6); `ProductionReadinessAuditTest` 3/3; release start audits clean.
- Two phones (125.7): A15 Sensor cut 60 s; the S21FE Monitor got every row by the reconnect flush; the A15 Monitor asked each hole once, then nothing.
- Not run: a live prune on the A15 (the next 15-minute prune with the old low run present shows it: no `Requested 788 to ...` line); 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.6: [SOT Count: 1012 (Rules: 512), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 401), QA: 133]

## 🎯 Next Resumption Focus
1. Check the follow-up docs commit above is pushed (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O100 parts 1, 3 and 4, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4 when convenient.

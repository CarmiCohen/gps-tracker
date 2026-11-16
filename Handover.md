```
git add .
git commit -m "Nov16.5 - MISSING MEANS STILL TO COME (ISSUE-NOV15.6-O100 parts 2 and 5)

- O100 part 2: the Monitor's Missing graph line counts only rows the Sensor may still send, not ranges it answered it no longer holds.
- O100 part 5: EventPeriod renamed ViewPeriod (own file). Parts 1, 3, 4 and the new part 6 stay open. Index row Nov16.4 filled (61de80e).
- TESTS: unit 849 PASSED; A15 chapter 198.2; ProductionReadinessAuditTest 3/3; release audits clean.
- VERSION: Nov16.5, versionCode 111."
git tag Nov16.5
git push origin main --tags
```

# Project Handover - Version Nov16.5 (Missing Means Still To Come)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov16.5^{commit}` (in PowerShell quote it: `'Nov16.5^{commit}'`).
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.5 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell).
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.5 debug Sensor `T` (service running) and Monitor `V`, paired, data kept. S21FE (`R5CRC14PG4F`): release Monitor Nov12.5, Global Mute on; **the debug Sensor Nov16.4 installed there by mistake in Nov16.4 is still there: the user removes it with `adb -s R5CRC14PG4F uninstall com.jd.app.sensor`**.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov16.5)
- ISSUE-NOV15.6-O100 parts 2 and 5 fixed; parts 1, 3, 4 and the new part 6 (prune widens a hole after a Sensor counter reset) open; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1 test --rerun-tasks`: PASSED (849); `assembleDebug`, `assembleRelease`, no new warning.
- A15: chapter 198.2 live (Missing 654,917 before, 0 after); `ProductionReadinessAuditTest` 3/3; release start audits clean.
- Not run: 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.5: [SOT Count: 1011 (Rules: 511), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 401), QA: 133]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. The user removes the debug Sensor from the S21FE (command above).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O100 parts 1, 3, 4 and 6, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4 when convenient.

```
git add .
git commit -m "Nov16.4 - THE SATELLITE LIST ON DEMAND (ISSUE-NOV15.5-O99 part 1)

- O99 part 1: the Sensor's Info always carries the satellite summary and the satellite list only while a Monitor's list is open (info_request 'sats', 15 s window); the dialog waits for the list.
- Parts 2 to 4 stay open. Index row Nov16.3 filled (914429f).
- TESTS: unit 845 PASSED; A15 chapter 197.3; ProductionReadinessAuditTest 3/3; release audits clean.
- VERSION: Nov16.4, versionCode 110."
git tag Nov16.4
git push origin main --tags
```

# Project Handover - Version Nov16.4 (The Satellite List On Demand)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov16.4^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.4 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell).
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.4 debug Sensor `T` (service running, reinstalled and restored from the Nov16.1 settings file; its event log, graph history and forensic buffer were lost) and Monitor `V`, paired. S21FE (`R5CRC14PG4F`): release Monitor Nov12.5, Global Mute on; **a debug Sensor Nov16.4 was installed there by mistake (19:41): the user removes it with `adb -s R5CRC14PG4F uninstall com.jd.app.sensor`**.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov16.4)
- ISSUE-NOV15.5-O99 part 1 fixed; parts 2 to 4 open; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1 test --rerun-tasks`: PASSED (845); `assembleDebug`, `assembleRelease`, no new warning.
- A15: chapter 197.3 live; `ProductionReadinessAuditTest` 3/3; release start audits clean.
- Not run: a clean traffic measurement (the A15's background traffic was high); 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.4: [SOT Count: 1010 (Rules: 510), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 399), QA: 132]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. The user removes the debug Sensor from the S21FE (command above).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O100, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4 when convenient.

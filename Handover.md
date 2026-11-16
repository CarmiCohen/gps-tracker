```
git add .
git commit -m "Nov16.3 - UNRECORDED SENSOR STOPS (ISSUE-NOV15.4-O98 parts 1 and 3)

- O98 part 3: on start the Sensor records the stop its previous run left out, with Android's exit reason and time (ApplicationExitInfo), else after its last event (phone restart or reason unknown); EventLog.recordFrom adds it at its own time.
- O98 part 1: verified not a defect (acknowledged alarm that clears records Ended); regression test added. Parts 2 and 4 stay open.
- Index row Nov16.2 filled (bdc2c0b).
- TESTS: unit 839 PASSED; A15 chapter 196.3 (app updated, force-stopped); ProductionReadinessAuditTest 3/3; release audits clean.
- VERSION: Nov16.3, versionCode 109."
git tag Nov16.3
git push origin main --tags
```

# Project Handover - Version Nov16.3 (Unrecorded Sensor Stops)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov16.3^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.3 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.3 debug Sensor `T` (service running) and Monitor `V`, paired; Sensor test APK Nov16.3, Monitor test APK Nov16.2. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; chapter 196.3 reads the Sensor's event file with `run-as ... cat`.

## 🟢 Status: PASSED (Nov16.3)
- ISSUE-NOV15.4-O98 part 3 fixed, part 1 verified (no defect); parts 2 and 4 open; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: PASSED (839); `assembleDebug`, `assembleRelease`, no new warning.
- A15: chapter 196.3 live (app updated, force-stopped); `ProductionReadinessAuditTest` 3/3; release start audits clean.
- Not run: the reboot and low-memory paths live (unit-tested only); full instrumented suites; 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.3: [SOT Count: 1009 (Rules: 509), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 396), QA: 131]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O99, O100, O86, O94 to O97, O98 parts 2 and 4 when convenient.

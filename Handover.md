```
git add -A
git commit -m "Nov17.2 - A SMALLER GRAPH SYNC, AND ONLY THE PAIRED SENSOR'S STOPS (ISSUE-NOV15.6-O100 parts 1 and 7)

- O100 part 1: graph samples are sent gzipped (Base64) to Monitors that ask for it; plain to older peers. User decisions: compress; answers to all Monitors of the room accepted.
- O100 part 7: received Sensor events carry the Sensor's ID, the Monitor shades only the paired Sensor's stops, and the Sensor sends its starts and stops with each graph answer. O100 closed.
- SOT 1.442; chapter 202.
- Tests: 857 unit tests PASSED; both builds PASSED; A15: compressed answer logged, ProductionReadinessAuditTest 3/3, release audit clean.
- Version Nov17.2 (117)."
git tag Nov17.2
git push origin main --tags
```

# Project Handover - Version Nov17.2 (A Smaller Graph Sync, And Only The Paired Sensor's Stops)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.2 waits for the Git block above; the index row Nov17.2 says `pending` (the next session fills it from the tag). Nov17.1 is `592ff8d`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.2 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `device_audit.cmd <serial> <package>`, `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; with both phones connected use the Two-phone test (`DOCS/issues.md` Operational Notes); when the effort level should change, stop and ask; `//` means "continue". 2026-11-17: graph answers compressed; answers reaching every Monitor of the room accepted.
- **Phones**: A15 (`R58X40GV2AR`): Nov17.2 debug Sensor and Monitor, paired, Sensor service running. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112), paired to `T`, Global Mute on; connected this session but untouched.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected; with two phones use `adb -s`, not the IDE's device tools. `run_tests.ps1` undercounts when tasks are up to date (O94): add `--rerun-tasks` for the full 857.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.2)
- ISSUE-NOV15.6-O100 parts 1 and 7 done, O100 closed; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 857 unit tests and both builds PASSED; A15: `samples sent (compressed)` logged, stops listed on the Monitor, `ProductionReadinessAuditTest` 3/3, release start audits clean; debug builds restored.

**Audit line**: Nov17.2: [SOT Count: 1016 (Rules: 516), Open: H:0, M:3, L:18, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 412), QA: 137]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.2 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O99 parts 2 to 4, O98 parts 2 and 4, O86, O94 to O97.

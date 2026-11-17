```
git add -A
git commit -m "Nov17.3 - BACKFILL AS COMPLETE AS LIVE, AND SINCE WHEN THE DROPOUTS COUNT (ISSUE-NOV15.5-O99 parts 2 to 4)

- O99 part 2: the forensic buffer (file version 4, upgraded in place from 2 and 3) keeps satellites, signal and noise; live and backfill rows share one encoder, so backfilled rows carry them.
- O99 part 3: Info shows 'Counting since' for the link statistics (user decision: keep them in memory, say since when).
- O99 part 4: light checked in a lit room on the A15 (203 lx vs 224-227 lx). O99 closed; new gap O102.
- SOT 1.443; chapters 203 and 204.
- Tests: 867 unit tests PASSED; both builds PASSED; A15: buffer upgraded, ProductionReadinessAuditTest 3/3, release audit clean.
- Version Nov17.3 (118)."
git tag Nov17.3
git push origin main --tags
```

# Project Handover - Version Nov17.3 (Backfill As Complete As Live, And Since When The Dropouts Count)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.3 waits for the Git block above; the index row Nov17.3 says `pending` (the next session fills it from the tag). Nov17.2 is `0b56fe7`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.3 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `install_release.ps1`, `device_audit.cmd <serial> <package>`, `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; with both phones connected use the Two-phone test (`DOCS/issues.md` Operational Notes); when the effort level should change, stop and ask; `//` means "continue". 2026-11-17: link statistics stay in memory, Info says since when they count.
- **Phones**: A15 (`R58X40GV2AR`): Nov17.3 debug Sensor and Monitor, paired, Sensor service running, forensic buffer at version 4. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112), paired to `T`, Global Mute on; connected this session but untouched.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected; with two phones use `adb -s`. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1` (no `MainActivity`). Add `--rerun-tasks` for the full 867 tests (O94).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.3)
- ISSUE-NOV15.5-O99 parts 2 to 4 done, O99 closed; new Low gap ISSUE-NOV17.3-O102 (backfilled rows without battery and temperature); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 867 unit tests and both builds PASSED; A15: real buffer upgraded (sequence continued), Info `Counting since` and `Light 203 lx`, `ProductionReadinessAuditTest` 3/3, release start audits clean; debug builds restored.

**Audit line**: Nov17.3: [SOT Count: 1017 (Rules: 517), Open: H:0, M:3, L:18, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 417), QA: 139]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.3 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O98 parts 2 and 4, O102, O86, O94 to O97.

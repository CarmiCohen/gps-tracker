```
git add .
git commit -m "Nov17.0 - SCALE AND VERSION ON THE MAP (ISSUE-NOV17.0-O101)

- O101: the Monitor's map shows a metric scale bar bottom right and the version name bottom left (also on a right-to-left phone).
- The old scale bar lived only in the unused OsmMap; its leftovers are logged as SIMP-NOV17.0-S1.
- Tests: 852 unit tests PASSED; UnpairConfirmationTest 11/11 on the A15 (+theMapShowsTheVersion); release audit clean.
- Version Nov17.0 (115)."
git tag Nov17.0
git push origin main --tags
```

# Project Handover - Version Nov17.0 (Scale And Version On The Map)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.0 waits for the Git block above; the index row Nov17.0 says `pending` (the next session fills it from the tag). Nov16.8 is `e3dad8f`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `device_audit.cmd <serial> <package>` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov17.0 debug Sensor and Monitor, paired, Sensor service running, ringer on vibrate, airplane mode off. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112), paired to `T`, Global Mute on; untouched.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.0)
- ISSUE-NOV17.0-O101 done (scale bottom right, version bottom left on the Monitor's map); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 852 unit tests and both builds PASSED, no new warning; A15: chapter 200 verified (test and screenshot), `ProductionReadinessAuditTest` 3/3, release start audits clean; debug builds restored.

**Audit line**: Nov17.0: [SOT Count: 1014 (Rules: 514), Open: H:0, M:3, L:19, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 406), QA: 135]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.0 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. SIMP-NOV17.0-S1 (remove the unused map classes) is a quick cleanup; then O100 parts 1 and 7, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4.

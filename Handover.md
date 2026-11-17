```
git add -A
git commit -m "Nov17.8 - RUN_TESTS.PS1 COUNTS EVERY TEST (ISSUE-NOV12.6-O94)

- O94: -PrerunTests (root build.gradle.kts) reruns every Test task without recompiling; run_tests.ps1 always passes it, cached field removed.
- Queue: user requests O104 to O109 logged (map, HUD, trails, fence, menu).
- Tests: 878 unit tests PASSED twice in a row, both builds; no app code, no device audit.
- Version Nov17.8 (123)."
git tag Nov17.8
git push origin main --tags
```

# Project Handover - Version Nov17.8 (run_tests.ps1 Counts Every Test)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.8 waits for the Git block above; the index row Nov17.8 says `pending` (the next session fills it from the tag). Nov17.7 is `95b9bdd`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.8 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1` (now always reruns the test tasks, so its count is complete without `--rerun-tasks`), `install_release.ps1` (run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.7, service running, buffer file version 5; Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.5 (120), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1`. A force-stopped Monitor does not stay offline: use airplane mode. The first launch right after a release install can show one long frame (ART not compiled yet); audit the next cold start.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.8)
- ISSUE-NOV12.6-O94 closed: `run_tests.ps1` reruns every requested test task (`-PrerunTests`), so a repeat run counts all 878 tests; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 878 unit tests (twice in a row) and both builds PASSED.
- Not run: device audit (build scripts only, no app code); 207.4 still waits for the S21FE.

**Audit line**: Nov17.8: [SOT Count: 1021 (Rules: 521), Open: H:0, M:3, L:21, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 429), QA: 143]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.8 from the tag (`/effort low`).
2. When the S21FE is connected: install the release Monitor Nov17.8 (new icon), and run 207.4 as a Two-phone test (`/effort low`).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O95 to O97, the user requests O104 to O109, then O86.

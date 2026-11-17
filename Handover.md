```
git add -A
git commit -m "Nov17.9 - THE SENSOR SHOWS AND OBEYS THE MONITOR'S SWITCHES FROM THE START (ISSUE-NOV15.2-O97)

- O97: the Tilt, Lift & Vibration line names the kinds switched off on the Monitor; a service start no longer opens the microphone before the stored detection settings are read.
- Tests: 880 unit tests and both builds PASSED; A15 checks 209.2 and 209.3; release Sensor audit clean.
- Version Nov17.9 (124)."
git tag Nov17.9
git push origin main --tags
```

# Project Handover - Version Nov17.9 (The Sensor Shows And Obeys The Monitor's Switches From The Start)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.9 waits for the Git block above; the index row Nov17.9 says `pending` (the next session fills it from the tag). Nov17.8 is `a75e943`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.9 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1` (always reruns the test tasks), `install_release.ps1` (run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.9, service running, stored detection settings with noise and vibration off (from the S21FE Monitor); Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.5 (120), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1`. A force-stopped Monitor does not stay offline: use airplane mode. The Sensor logs with the `REV_` tag prefix. The first launch right after a release install can show one long frame (ART not compiled yet); audit the next cold start.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.9)
- ISSUE-NOV15.2-O97 closed: the Sensor's motion line names the kinds switched off on the Monitor, and the microphone stays closed from the start when noise is off; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 880 unit tests and both builds PASSED; A15: 209.2 and 209.3, `ProductionReadinessAuditTest` 3/3, release Sensor audit clean.
- Not run: Monitor audit (not installed on the A15, code unchanged); 207.4 still waits for the S21FE.

**Audit line**: Nov17.9: [SOT Count: 1022 (Rules: 522), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 432), QA: 144]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.9 from the tag (`/effort low`).
2. When the S21FE is connected: install the release Monitor Nov17.9 (new icon), run 207.4 as a Two-phone test, then O95 (`/effort low`, high for the O95 fix).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O96 needs a user decision (store the STOP or reword the dialog); then the user requests O104 to O109, then O86.

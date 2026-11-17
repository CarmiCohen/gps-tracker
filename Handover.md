```
git add -A
git commit -m "Nov17.11 - STATE ALARMS END WHEN CLEARED (ISSUE-NOV17.10-O110)

- O110: a state alarm (offline, power, battery, temperature, relay, internet, GPS signal, noise detection off) ends its siren and red screen when its condition clears; event alarms stay until acknowledged.
- Docs: O96 decision (reword the STOP dialog), new issues O110 and O111; index row Nov17.9 filled.
- Tests: 888 unit tests and both builds PASSED; A15 MonitorAlarmDeviceTest 5/5, ProductionReadinessAuditTest 3/3, release audits clean.
- Version Nov17.11 (125); Nov17.10 was not committed."
git tag Nov17.11
git push origin main --tags
```

# Project Handover - Version Nov17.11 (State Alarms End When Cleared)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.11 waits for the Git block above; the index row Nov17.11 says `pending` (the next session fills it from the tag). Nov17.9 is `92e35ab`; Nov17.10 was planned but never committed (no tag).
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.11 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell); if a test compile reports unresolved top-level functions that exist, run `:app-monitor:clean` (stale output, Nov17.11). `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.11, service running, stored detection settings with noise and vibration off; Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.5 (120), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the release Monitor with `am start -n com.jd.app.monitor/.MonitorActivity` (a `monkey` launch did not start it in Nov17.11). A force-stopped Monitor does not stay offline: use airplane mode. The Sensor logs with the `REV_` tag prefix. The first launch right after a release install can show one long frame; audit the next cold start.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.11)
- ISSUE-NOV17.10-O110 closed: state alarms end with their condition, event alarms stay latched until acknowledged; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 888 unit tests and both builds PASSED; A15: `MonitorAlarmDeviceTest` 5/5 (the first run crashed the test process, likely O90), `ProductionReadinessAuditTest` 3/3, release Sensor and Monitor audits clean.
- Not run: 210.3 (live power unplug with a paired Monitor) and 207.4: both wait for a Two-phone test with the S21FE.

**Audit line**: Nov17.11: [SOT Count: 1023 (Rules: 523), Open: H:0, M:3, L:21, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 435), QA: 145]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.11 from the tag; then O111, debounce the Sensor's event alerts (analyse recent false alarms first; `/effort high`).
2. When the S21FE is connected: install the release Monitor Nov17.11, run 210.3 and 207.4 as a Two-phone test, then O95 (`/effort low`, high for the O95 fix).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O96: reword the STOP dialog (user decision 2026-11-17: no stored stop; `/effort low`); then the user requests O104 to O109, then O86.

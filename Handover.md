```
git add -A
git commit -m "Nov17.12-debounce - THE SENSOR DEBOUNCES ITS EVENT ALERTS (ISSUE-NOV17.10-O111)

- O111: vibration needs 3 hits within 10 s, tilt lasts 2 s, a lift is held 1 s, noise is loud 1 s within 2 s; one alert per kind a minute (AlertCoolDown).
- Docs: new gap O112 (check the numbers against the S21FE's Events); index row Nov17.11 filled.
- Tests: 897 unit tests and both builds PASSED; A15 ProductionReadinessAuditTest 3/3, release Sensor audit clean.
- Version Nov17.12-debounce (126)."
git tag Nov17.12-debounce
git push origin main --tags
```

# Project Handover - Version Nov17.12-debounce (The Sensor Debounces Its Event Alerts)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.12-debounce waits for the Git block above; the index row Nov17.12-debounce says `pending` (the next session fills it from the tag). Nov17.11 is `c3775cd`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.12-debounce needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell); if a test compile reports unresolved top-level functions that exist, run `:app-monitor:clean`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor (built just before the version bump, so it shows Nov17.11), service running, stored detection settings with noise and vibration off; Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.5 (120), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the release Monitor with `am start -n com.jd.app.monitor/.MonitorActivity`. A force-stopped Monitor does not stay offline: use airplane mode. The Sensor logs with the `REV_` tag prefix; its event log is `/data/user_de/0/com.jd.app.sensor/files/event_log.jsonl` (`adb exec-out run-as com.jd.app.sensor cat ...`).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.12-debounce)
- ISSUE-NOV17.10-O111 closed: the Sensor debounces noise, tilt, lift and vibration and sends one alert per kind a minute; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 897 unit tests and both builds PASSED; A15: `ProductionReadinessAuditTest` 3/3, release Sensor audit clean (Monitor unchanged, not audited).
- Not run: 211.2 (live knocks, tilt, lift, noise: needs a person at the A15); 210.3 and 207.4 wait for a Two-phone test.
- New gap O112: the numbers come from the A15's own log; check them against the S21FE Monitor's Events.

**Audit line**: Nov17.12-debounce: [SOT Count: 1024 (Rules: 524), Open: H:0, M:3, L:21, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 437), QA: 146]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.12-debounce from the tag (`/effort low`).
2. When the S21FE is connected: install the release Monitor, run 210.3 and 207.4 as a Two-phone test, read its Events for O112, then O95 (`/effort low`, high for the O95 fix).
3. With a person at the A15: chapter 211.2 (`/effort low`).
4. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
5. O96: reword the STOP dialog (user decision 2026-11-17; `/effort low`); then the user requests O104 to O109, then O86.

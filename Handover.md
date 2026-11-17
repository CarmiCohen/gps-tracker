```
git add -A
git commit -m "Nov17.14 - TAPPING AN APP IN ANDROID'S ACTIVE APPS LIST OPENS IT (ISSUE-NOV17.14-O113)

- O113: the Monitor's session notification opens the Monitor like the launcher (new monitorOpenIntent, buildSessionNotification); the Sensor's already opened its console.
- Rule 17: no test is removed without the user's explicit confirmation; index row Nov17.13 filled (19c4f74).
- Tests: 897 unit tests and both builds PASSED; A15 SessionNotificationDeviceTest 2/2, release Monitor audit clean.
- Version Nov17.14 (128)."
git tag Nov17.14
git push origin main --tags
```

# Project Handover - Version Nov17.14 (Tapping an App in Android's Active Apps List Opens It)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.14 waits for the Git block above; the index row Nov17.14 says `pending` (the next session fills it from the tag). Nov17.13 is `19c4f74`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.14 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell); if a test compile reports unresolved top-level functions that exist, run `:app-monitor:clean`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`. `ProductionReadinessAuditTest` exists only in the Sensor.
- **User decisions**: Rule 17: never remove a test (TEST_PROCEDURE chapter, sub-item, or automated test) without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored (O96); notification tap only, no action buttons (O113).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor (shows Nov17.12-debounce), service running, stored detection settings with noise and vibration off; Monitor UNINSTALLED (reinstall only for an A15-only test; a fresh Monitor asks for location first: `pm grant` it). S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.5 (120), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the release Monitor with `am start -n com.jd.app.monitor/.MonitorActivity`. A force-stopped Monitor does not stay offline: use airplane mode. The Sensor logs with the `REV_` tag prefix; its event log is `/data/user_de/0/com.jd.app.sensor/files/event_log.jsonl` (`adb exec-out run-as com.jd.app.sensor cat ...`).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.14)
- ISSUE-NOV17.14-O113 closed: a tap on the Monitor's session notification (shade or Active apps list) opens the Monitor; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 897 unit tests and both builds PASSED; A15: `SessionNotificationDeviceTest` 2/2, release Monitor cold start clean; Sensor tap checked live (opens its console).
- Not run: 213.2 (live tap on a paired Monitor: no session notification while unpaired); 212.1, 211.2 (need a person at the A15); 210.3 and 207.4 wait for a Two-phone test.

**Audit line**: Nov17.14: [SOT Count: 1026 (Rules: 526), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 440), QA: 148]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.14 from the tag (`/effort low`).
2. When the S21FE is connected: install the release Monitor, run 213.2, 210.3 and 207.4 as a Two-phone test, read its Events for O112, then O95 (`/effort low`, high for the O95 fix).
3. With a person at the A15: chapters 211.2 and 212.1 (`/effort low`).
4. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
5. The user requests O104 to O109 (`/effort medium`), then O86.

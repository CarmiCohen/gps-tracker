```
git add -A
git commit -m "Nov18.0 - REOPENING THE SENSOR APP RESTARTS A STOPPED SERVICE (ISSUE-NOV18.0-O115)

- O115: ServiceStartOnOpen starts SensorService at every return of the console to the front (not after a rotation); before, only a fresh launch did, so Home and the launcher icon left a stopped Sensor stopped (found by chapter 212.1).
- Index row Nov17.16 filled (d8d25c7).
- Tests: run_tests 919 PASSED, both builds; ServiceStartOnOpenTest 5/5; A15: 212.1 and 216.2 passed, ProductionReadinessAuditTest 3/3, release Sensor audit clean.
- Version Nov18.0 (131)."
git tag Nov18.0
git push origin main --tags
```

# Project Handover - Version Nov18.0 (Reopening The Sensor App Restarts A Stopped Service)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.0 waits for the Git block above; the index row Nov18.0 says `pending` (the next session fills it from the tag). Nov17.16 is `d8d25c7`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.0 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `"-Pkotlin.compiler.execution.strategy=in-process"` (quoted in PowerShell); if a compile reports unresolved top-level functions that exist, run `gradlew --stop` and `:app-monitor:clean :core-network:clean`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored, and any return to the console restarts the service (O96, O115); notification tap only (O113); the power alarm follows plugged in, not charging (O114).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.0, service running, on USB with battery held at 80%; Monitor and both test apks not installed; rotation settings unchanged (auto-rotate on). S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.15 (129), paired to `T`, Global Mute on, airplane off (untouched).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Sensor console: tap the `System Idle` screen to unlock; `STOP SERVICE` moves with the motion line (read its bounds first); the dialog's confirm is at `395 1445`. S21FE Monitor taps (1080x2340): wrench `1020 156` (Setup), then `ALERTS` `763 1603`, Global Mute switch `134 1436`; Events `756 156`; the X at the far left opens `Unpair` (cancel with Back). Power simulation on the A15: `dumpsys battery unplug` / `set status 2` / `reset`.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.0)
- ISSUE-NOV18.0-O115 opened and closed: chapter 212.1 on the A15 showed that Home and the launcher icon did not restart a stopped Sensor; `ServiceStartOnOpen` now starts it at every return of the console, not after a rotation; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: 919 tests PASSED, both builds, no new warning; A15: `ProductionReadinessAuditTest` 3/3, release Sensor audit `davey_over_500ms=0 stacklog=0 fatal=0`, 212.1 and 216.2 passed.
- Not run: Monitor audit (Monitor unchanged; S21FE disconnected); 211.2 and 213.2's Active apps list tap (need a person at the A15); 215.1 and O112 (S21FE).

**Audit line**: Nov18.0: [SOT Count: 1029 (Rules: 529), Open: H:0, M:3, L:19, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 445), QA: 151]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.0 from the tag (`/effort low`).
2. With a person at the A15: chapters 211.2 and 213.2's Active apps list tap (`/effort low`).
3. When the S21FE is connected: chapter 215.1, the full Monitor run (airplane on, restore after); after a few days, read its Events for O112 (`/effort low`, medium for O112).
4. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
5. The user requests O104 to O109 (`/effort medium`), then O86.

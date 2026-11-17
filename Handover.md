```
git add -A
git commit -m "Nov17.15 - A PLUGGED-IN PHONE THAT IS NOT CHARGING HAS POWER (ISSUE-NOV17.15-O114)

- O114: the Sensor sends plugged in (plg, EXTRA_PLUGGED) apart from charging; Tracker power disconnected and the sampling rate follow it; older rows fall back to charging (BatteryPower.kt).
- Two-phone test: 210.3 and 207.4 passed, 213.2 shade tap passed (Active apps list not run); O112 waits for more S21FE Events.
- Index row Nov17.14 filled (c705fbb).
- Tests: 909 unit tests and both builds PASSED; ProductionReadinessAuditTest 3/3; release Sensor (A15) and Monitor (S21FE) audits clean; 214.2 live on both phones.
- Version Nov17.15 (129)."
git tag Nov17.15
git push origin main --tags
```

# Project Handover - Version Nov17.15 (A Plugged-In Phone That Is Not Charging Has Power)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.15 waits for the Git block above; the index row Nov17.15 says `pending` (the next session fills it from the tag). Nov17.14 is `c705fbb`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.15 needed no relay change (`plg` passes through).
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`; if a compile reports unresolved top-level functions that exist (Nov17.15: `peerDeliveries`), run `gradlew --stop` and `:app-monitor:clean :core-network:clean`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored (O96); notification tap only (O113); the power alarm follows plugged in, not charging (O114).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.15, service running, on USB with battery held at 80% (status 4, not charging); Monitor UNINSTALLED. S21FE (`R5CRC14PG4F`): release Monitor Nov17.15 (129), paired to `T`, Global Mute on, airplane off.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. S21FE Monitor taps (1080x2340): wrench `1020 156` (Setup), then `ALERTS` `763 1603`, Global Mute switch `134 1436`; Events `756 156`; the X at the far left opens `Unpair` (cancel with Back). Power simulation on the A15: `dumpsys battery unplug` / `set status 2` / `reset`.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.15)
- ISSUE-NOV17.15-O114 opened and closed: a phone on a charger that holds its battery no longer raises `Tracker power disconnected`; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Two-phone test: 210.3, 207.4 and 214.2 passed; 213.2 shade tap passed, the Active apps list tap not run (entry not found in One UI 8 quick settings).
- 909 unit tests and both builds PASSED; `ProductionReadinessAuditTest` 3/3; release audits clean on both phones.
- Not run: O112 (too few S21FE Events since the debounce Sensor); 212.1, 211.2 (need a person at the A15).

**Audit line**: Nov17.15: [SOT Count: 1027 (Rules: 527), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 442), QA: 149]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.15 from the tag (`/effort low`).
2. When the S21FE is connected: O95 (`/effort high` for the fix); after a few days, read its Events for O112 (`/effort medium`).
3. With a person at the A15: chapters 211.2 and 212.1, and 213.2's Active apps list tap (`/effort low`).
4. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
5. The user requests O104 to O109 (`/effort medium`), then O86.

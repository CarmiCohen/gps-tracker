```
git add -A
git commit -m "Nov17.16 - A TEST TELEMETRY UPDATE IS NOT LOST BEFORE THE REPOSITORY LISTENS (ISSUE-NOV12.6-O95)

- O95: FakeSignalingClient waits for a collector before emitting and fails on a dropped value; the fresh-pairing backfill test no longer times out when MonitorRepository's IO collector starts late (test code only).
- Index row Nov17.15 filled (007d4d4).
- Tests: full Monitor am instrument on the A15 OK (63 tests); :core-shared:test PASSED; S21FE run not done (disconnected).
- Version Nov17.16 (130)."
git tag Nov17.16
git push origin main --tags
```

# Project Handover - Version Nov17.16 (A Test Telemetry Update Is Not Lost Before The Repository Listens)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.16 waits for the Git block above; the index row Nov17.16 says `pending` (the next session fills it from the tag). Nov17.15 is `007d4d4`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.16 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`; if a compile reports unresolved top-level functions that exist, run `gradlew --stop` and `:app-monitor:clean :core-network:clean`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored (O96); notification tap only (O113); the power alarm follows plugged in, not charging (O114).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.15, service running, on USB with battery held at 80%; Monitor and its test apk UNINSTALLED again after the run. S21FE (`R5CRC14PG4F`): disconnected during the session; release Monitor Nov17.15 (129), paired to `T`, Global Mute on, airplane off (untouched).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. S21FE Monitor taps (1080x2340): wrench `1020 156` (Setup), then `ALERTS` `763 1603`, Global Mute switch `134 1436`; Events `756 156`; the X at the far left opens `Unpair` (cancel with Back). Power simulation on the A15: `dumpsys battery unplug` / `set status 2` / `reset`.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.16)
- ISSUE-NOV12.6-O95 closed: the fake client's `tryEmit` dropped the first update before `MonitorRepository` collected; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Docs-only version (test code only, D5): full Monitor `am instrument` on the A15 OK (63 tests); `:core-shared:test` PASSED; no builds or device audit.
- Not run: chapter 215.1 on the S21FE (disconnected; user chose to close); O112, 211.2, 212.1 as before.

**Audit line**: Nov17.16: [SOT Count: 1028 (Rules: 528), Open: H:0, M:3, L:19, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 443), QA: 150]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.16 from the tag (`/effort low`).
2. When the S21FE is connected: chapter 215.1, the full Monitor run (airplane on, restore after); after a few days, read its Events for O112 (`/effort low`, medium for O112).
3. With a person at the A15: chapters 211.2 and 212.1, and 213.2's Active apps list tap (`/effort low`).
4. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
5. The user requests O104 to O109 (`/effort medium`), then O86.

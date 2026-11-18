```
git add -A
git commit -m "Nov18.4-noise - A NOISE MUST LAST TWO SECONDS (ISSUE-NOV17.12-O112)

- O112 (noise part): ACOUSTIC_CONFIRM_MS 2000, ACOUSTIC_CONFIRM_WINDOW_MS 3000; cough bursts no longer alert, a 2 s coughing fit still can (accepted).
- Tests: AcousticDetectorTest +1 (aCoughingFitDoesNotAlert), gaps and timing tests follow the constants.
- A15 trial with a temporary level log (removed): talking and claps detected, cough bursts not.
- Index row Nov18.3 filled (8b3963e).
- Tests: run_tests 922 PASSED, both builds; A15 release audits of both apps clean.
- Version Nov18.4-noise (135)."
git tag Nov18.4-noise
git push origin main --tags
```

# Project Handover - Version Nov18.4-noise (A Noise Must Last Two Seconds)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.4-noise waits for the Git block above; the index row Nov18.4-noise says `pending` (the next session fills it from the tag). Nov18.3 is `8b3963e`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.4-noise needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or report unresolved functions that exist, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored, and any return to the console restarts the service (O96, O115); notification tap only (O113); the power alarm follows plugged in, not charging (O114); a noise must be loud 2 s within 3 s, and a long coughing fit that still alerts is accepted (O112, 2026-11-18, built in Nov18.4-noise).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.3 code plus the O112 change (built before the version bump), service running, on USB with battery held at 80%; Monitor and the test apk not installed (installed for the audit, then removed); auto-rotate on. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.15 (129), paired to `T`, Global Mute on, airplane off (untouched).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Sensor console: tap the `System Idle` screen once to unlock (the first tap may only wake the screen; a second tap can hit the wrench and open Phone Setup, X at `132 230`); `STOP SERVICE` moves with the motion line (read its bounds first); the dialog's confirm is at `395 1445`. S21FE Monitor taps (1080x2340): wrench `1020 156` (Setup), then `ALERTS` `763 1603`, Global Mute switch `134 1436`; Events `756 156`; the X at the far left opens `Unpair` (cancel with Back). Power simulation on the A15: `dumpsys battery unplug` / `set status 2` / `reset`. Noise levels: no level log in the code; for a measurement add a temporary `Log.d` in `AcousticMonitor.listen` (as in Nov18.4-noise) and remove it before closing.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.4-noise)
- ISSUE-NOV17.12-O112 noise part done (O112 stays open for vibration, tilt and lift): a noise alerts only when loud 2 s within 3 s; checked with the user at the A15 (chapter 220 L/A); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: 922 tests PASSED, both builds, no new warning; A15: `ProductionReadinessAuditTest` OK, release Sensor and Monitor audits `davey_over_500ms=0 stacklog=0 fatal=0`.
- Not run: 218.2 and 217.2 (need a paired Monitor; S21FE disconnected); 211.2, 213.2's Active apps list tap (need a person at the A15); 215.1 (S21FE).

**Audit line**: Nov18.4-noise: [SOT Count: 1033 (Rules: 533), Open: H:0, M:3, L:16, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 453), QA: 155]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.4-noise from the tag (`/effort low`).
2. The user requests O107 to O109, then O86 (`/effort medium`).
3. When the S21FE is connected: chapters 215.1 (full Monitor run, airplane on, restore after), 217.2 and 218.2 (look at the map and HUD); read its Events for O112's other kinds (`/effort low`).
4. With a person at the A15: chapters 211.2 and 213.2's Active apps list tap; at the next Manual Deploy on Render: chapter 182.2 (`/effort low`).

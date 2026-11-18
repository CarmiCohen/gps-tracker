```
git add -A
git commit -m "Nov18.3 - BATTERY AND THERMOMETER ICONS IN THE HUD (ISSUE-NOV17.7-O106)

- O106: the HUD telemetry line reads '🔋42%⚡|🌡️34°|A:...|AGE:...'; the B: and T: labels and the C are gone.
- Docs: ALARM_AND_SIREN_MECHANISM.md 3a and chapter 129.2 follow O110 (state alarms end when cleared).
- O112: coughing raised noise alarms; user decision: about 2 s loud within 3 s (next version).
- Index row Nov18.2 filled (0c43995).
- Tests: run_tests 921 PASSED, both builds; A15 release audits of both apps clean.
- Version Nov18.3 (134)."
git tag Nov18.3
git push origin main --tags
```

# Project Handover - Version Nov18.3 (Battery And Thermometer Icons In The HUD)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.3 waits for the Git block above; the index row Nov18.3 says `pending` (the next session fills it from the tag). Nov18.2 is `0c43995`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.3 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or report unresolved functions that exist, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored, and any return to the console restarts the service (O96, O115); notification tap only (O113); the power alarm follows plugged in, not charging (O114); a noise must be loud about 2 s within 3 s (O112, 2026-11-18, not built yet).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.3, service running, on USB with battery held at 80%; Monitor and the test apk not installed (installed for the audit, then removed); auto-rotate on. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.15 (129), paired to `T`, Global Mute on, airplane off (untouched).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Sensor console: tap the `System Idle` screen once to unlock (the first tap may only wake the screen; a second tap can hit the wrench and open Phone Setup, X at `132 230`); `STOP SERVICE` moves with the motion line (read its bounds first); the dialog's confirm is at `395 1445`. S21FE Monitor taps (1080x2340): wrench `1020 156` (Setup), then `ALERTS` `763 1603`, Global Mute switch `134 1436`; Events `756 156`; the X at the far left opens `Unpair` (cancel with Back). Power simulation on the A15: `dumpsys battery unplug` / `set status 2` / `reset`.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.3)
- ISSUE-NOV17.7-O106 closed: the HUD marks battery and temperature with 🔋 and 🌡️, no `B:`, `T:` or `C`; seen on the A15 (chapter 219 L/A); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Docs: the alarm-ending rule in `ALARM_AND_SIREN_MECHANISM.md` 3a and chapter 129.2 now follows O110; O112 holds the user's cough false alarms and the noise decision.
- `run_tests.ps1`: 921 tests PASSED, both builds, no new warning; A15: `ProductionReadinessAuditTest` OK, release Sensor and Monitor audits `davey_over_500ms=0 stacklog=0 fatal=0`.
- Not run: 218.2 and 217.2 (need a paired Monitor; S21FE disconnected); 211.2, 213.2's Active apps list tap (need a person at the A15); 215.1 (S21FE).

**Audit line**: Nov18.3: [SOT Count: 1032 (Rules: 532), Open: H:0, M:3, L:16, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 451), QA: 154]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.3 from the tag (`/effort low`).
2. O112's noise change: a noise alerts only when loud about 2 s within 3 s (`ACOUSTIC_CONFIRM_MS`, `ACOUSTIC_CONFIRM_WINDOW_MS` in `AcousticDetector.kt`), with tests and a chapter; check against coughs at the A15 (`/effort high`).
3. Then the user requests O107 to O109, then O86 (`/effort medium`).
4. When the S21FE is connected: chapters 215.1 (full Monitor run, airplane on, restore after), 217.2 and 218.2 (look at the map and HUD); read its Events for O112's other kinds (`/effort low`).
5. With a person at the A15: chapters 211.2 and 213.2's Active apps list tap; at the next Manual Deploy on Render: chapter 182.2 (`/effort low`).

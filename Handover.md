```
git add -A
git commit -m "Nov18.5 - SAVE, LOAD AND CLEAR TRAILS ON THE MAP (ISSUE-NOV17.7-O107)

- O107: map buttons in rows of two with CLEAR, LOAD, SAVE; a GPS Monitor trails file through the system picker.
- O107: a cleared trail stays cleared (TrailClear mark: no older row of the Sensor, no missing-start backfill).
- Messages say points, not rows (user decision 2026-11-18).
- Tests: MonitorTrailFileTest +6; A15 trial SAVE 10176, CLEAR held 80 s, LOAD 10176.
- Index row Nov18.4-noise filled (3b6f8c9).
- Tests: run_tests 934 PASSED, both builds; A15 audits and Monitor instrumented 64 OK.
- Version Nov18.5 (136)."
git tag Nov18.5
git push origin main --tags
```

# Project Handover - Version Nov18.5 (Save, Load And Clear Trails On The Map)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.5 waits for the Git block above; the index row Nov18.5 says `pending` (the next session fills it from the tag). Nov18.4-noise is `3b6f8c9`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.5 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or report unresolved functions that exist, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`. The IDE often shows false "unresolved" errors in new files; trust the Gradle build.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored, and any return to the console restarts the service (O96, O115); notification tap only (O113); the power alarm follows plugged in, not charging (O114); a noise must be loud 2 s within 3 s (O112); user-facing texts say "points", not "rows" (2026-11-18, Nov18.5).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.4-noise build of the same Sensor code (Nov18.5 changed only the Monitor), service running, on USB with battery held at 80%; Monitor and the test apks not installed. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov17.15 (129), paired to `T`, Global Mute on, airplane off (untouched).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'`. A debug Monitor on the A15 pairs with `T` from the wizard (`Pair Device` at `540 1024`, after the location and notification prompts); map buttons (RTL): SAVE `807 1550`, LOAD `982 1550`, CLEAR `807 1415`; the picker's save button `179 2100`; the Monitor event log: `run-as com.jd.app.monitor` `files/*.jsonl`. Sensor console and S21FE taps: see `DOCS/issues.md` Operational Notes and the Nov18.4-noise Handover (tag).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.5)
- ISSUE-NOV17.7-O107 done: map CLEAR, LOAD, SAVE; a clear is no longer refilled by backfill (found and fixed on the way); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: 934 tests PASSED, both builds, no new warning; A15: `ProductionReadinessAuditTest` OK, Monitor instrumented 64 OK, release Sensor and Monitor audits `davey_over_500ms=0 stacklog=0 fatal=0`.
- Not run: 218.2, 217.2, 215.1 (need the S21FE); 211.2, 213.2's Active apps list tap (need a person at the A15).

**Audit line**: Nov18.5: [SOT Count: 1034 (Rules: 534), Open: H:0, M:3, L:15, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 455), QA: 156]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.5 from the tag (`/effort low`).
2. The user requests O108 (show or hide the fence) and O109 (the menu's setup icons), then O86 (`/effort medium`).
3. When the S21FE is connected: chapters 215.1, 217.2, 218.2 and 221.2 on its Monitor (the S21FE runs the Nov17.15 Monitor; install the current release first, with the user's consent); read its Events for O112's other kinds (`/effort low`).
4. With a person at the A15: chapters 211.2 and 213.2's Active apps list tap; at the next Manual Deploy on Render: chapter 182.2 (`/effort low`).

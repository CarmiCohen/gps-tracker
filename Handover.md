# Project Handover - Version Nov25.25 (Knocks No Lift, Events Filter, Debounces)

## ▶️ Resumption State (Read First)
- **Git**: Nov25.25 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `:core-shared:animalsnifferMain` fails the build on a JDK call Android 26 lacks. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; SIMP-NOV9.3-S1 low, best after O72; O140 very low (now holds O162's combined-event cases). 2026-11-26 (O162): vibration 4 hits in 10 s; noise stays 2 s within 3 s (3 s in 10 s let a 5 s coughing fit alert); MOVING by GPS only, no accelerometer. Device tests one step at a time, answered by `//`.
- **Wire**: install both apps at Nov23.4 or later together. The Sensor sends `SIT_ALERT` since Nov25.23. Nov25.25 changes no wire field: the Monitor derives the Sensor's MOVING from its rows (`MovementTracker`).
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard`): debug Sensor Nov25.25 code (installed by `adb install -r`, version name still built as Nov25.24) running as `T`, lying on the chair seat after the pickup test (put it back where the user keeps it); no Monitor. S21FE (`R5CRC14PG4F`, not connected): release Monitor Nov25.16, Monitor `V`, `Tracker ID: T2` (do not re-pair). **Always check the S21FE's foreground app right before every tap.** With both connected, never `gradlew installDebug`: `adb -s <serial> install -r`, or `TESTS\install_release.ps1 -Serial <serial> -Apps monitor`.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). Lift tuning: `REV_Motion` `Lift not confirmed: ...` (a knock voided it, the speed never returned, or up to N cm); sit tuning: `Sit trace` and `Sit not confirmed (<why>)`.

## 🟢 Status: PASSED (Nov25.25)
- ISSUE-NOV25.23-O154 (knocks no lift; 257.1 and 257.2 passed on the A15), ISSUE-NOV25.25-O156 (Events: Alarms/Main/All, no row gaps, landscape column), ISSUE-NOV25.25-O162 (vibration 4 hits, MOVING debounced, combined events to O140).
- Unit tests and both builds: see the commit. Not run: 252.3 (S21FE look), 257.3 (a walk with the A15), both androidTests (only call sites changed, compiled), the release device audit (not needed: no start-up or service change).

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: a queue row of the user's choice, chapter 257.3, the S21FE look; item 3: the relay's return).

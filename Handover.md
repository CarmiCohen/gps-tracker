# Project Handover - Version Nov26.0 (Doc Sit)

## ▶️ Resumption State (Read First)
- **Git**: Nov26.0 (docs only) committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `:core-shared:animalsnifferMain` fails the build on a JDK call Android 26 lacks. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; SIMP-NOV9.3-S1 low, best after O72; O140 very low (now holds O162's combined-event cases). 2026-11-26 (O162): vibration 4 hits in 10 s; noise stays 2 s within 3 s (3 s in 10 s let a 5 s coughing fit alert); MOVING by GPS only, no accelerometer. Device tests one step at a time, answered by `//`. 2026-11-26 (sit): O155 next, starting with a chair measuring session; then O163 (placement, learning, feedback: row 96); a placement is where the phone is hidden, not where it is (a car parked at home uses the car's examples).
- **Wire**: install both apps at Nov23.4 or later together. The Sensor sends `SIT_ALERT` since Nov25.23. Nov25.25 changes no wire field: the Monitor derives the Sensor's MOVING from its rows (`MovementTracker`).
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard`): debug Sensor Nov25.25 code (installed by `adb install -r`, version name still built as Nov25.24) running as `T`, back under the chair cushion (user, 2026-11-26); no Monitor. S21FE (`R5CRC14PG4F`, not connected): release Monitor Nov25.16, Monitor `V`, `Tracker ID: T2` (do not re-pair). **Always check the S21FE's foreground app right before every tap.** With both connected, never `gradlew installDebug`: `adb -s <serial> install -r`, or `TESTS\install_release.ps1 -Serial <serial> -Apps monitor`.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). Lift tuning: `REV_Motion` `Lift not confirmed: ...` (a knock voided it, the speed never returned, or up to N cm); sit tuning: `Sit trace` and `Sit not confirmed (<why>)`.

## 🟢 Status: PASSED (Nov26.0)
- Docs only (D5): new row 96 ISSUE-NOV26.0-O163 (sit for many seats: placement, learning, judging, feedback); row 88 (O155) starts with the chair measuring session; row 83 (O150) 254.3 first as a recording session in a parked car at home.
- `:core-shared:test` only (D5); no builds, no device check. Nov25.25's code work: see its commit.

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: O155 with the chair measuring session, then O163; item 3: the relay's return).

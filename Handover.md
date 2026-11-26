# Project Handover - Version Nov26.3 (Sitting Period)

## ▶️ Resumption State (Read First)
- **Git**: Nov26.3 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `:core-shared:animalsnifferMain` fails the build on a JDK call Android 26 lacks. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; SIMP-NOV9.3-S1 low, best after O72; O140 very low. 2026-11-26: vibration 4 hits in 10 s; noise 2 s within 3 s; MOVING by GPS only; device tests one step at a time, answered by `//`. Nov26.3: a sit is `Sit confirmed` (movement within 1 min) or `Sit unconfirmed` (an empty seat or a still sitter), the period stays open; it ends 2 min after a stand-up without movement, or after 15 min without movement.
- **Wire**: install both apps at Nov23.4 or later together. The Sensor sends `SIT_ALERT` since Nov25.23; the period's confirmation and end are ordinary Sensor INFO events (no wire change). The Monitor derives the Sensor's MOVING from its rows (`MovementTracker`).
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard`): debug Sensor with the Nov26.3 code (name Nov26.1) running as `T`, under the chair cushion; no Monitor. S21FE (`R5CRC14PG4F`, not connected): release Monitor Nov25.16, Monitor `V`, `Tracker ID: T2` (do not re-pair). **Always check the S21FE's foreground app right before every tap.** With both connected, never `gradlew installDebug`: `adb -s <serial> install -r`, or `TESTS\install_release.ps1 -Serial <serial> -Apps monitor`.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). Sit data: stream `adb logcat -v time -s REV_Motion:* REV_Event:*` to a PC file (cmd redirection); inside a period `REV_Motion` logs a `Stillness 10 s` line per window (Nov26.3 data: `TESTS/data/Nov26.3_stillness.log`).

## 🟢 Status: PASSED (Nov26.3)
- ISSUE-NOV25.23-O155: sitting period (`StillnessMeter`, `SitPeriodEvent`, `MotionSentinel`, `SensorService`); chair stillness session and chapter 258.2 passed on the A15; its limits are an Open Gap.
- All unit tests, assembleDebug and assembleRelease; no device audit (no start-up, service-lifecycle or first-screen change).

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: O163, `/effort high`, and chapter 258.3 on the A15; item 3: the relay's return).

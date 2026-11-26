# Project Handover - Version Nov26.25 (Sit Learning)

## ▶️ Resumption State (Read First)
- **Git**: Nov26.25 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `:core-shared:animalsnifferMain` fails the build on a JDK call Android 26 lacks. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; SIMP-NOV9.3-S1 low, best after O72; O140 very low. 2026-11-26: vibration 4 hits in 10 s; noise 2 s within 3 s; MOVING by GPS only; device tests one step at a time, answered by `//`. Nov26.3: the sitting period rules. O163 is done in parts; Nov26.25's slice (learning) chosen by Claude with the user away.
- **Wire**: install both apps at Nov23.4 or later together. `sensor_behaviour` carries `placeId`, `placeName`, `placeType` (Nov26.4); new in Nov26.25: `sit_learn` (Monitor to Sensor) and `sit_examples` (Sensor to Monitor), ignored by older apps. The Monitor derives the Sensor's MOVING from its rows (`MovementTracker`).
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard`): debug Sensor with the Nov26.3 code (name Nov26.1) running as `T`, under the chair cushion; no Monitor; Nov26.4 and Nov26.25 not installed. S21FE (`R5CRC14PG4F`, not connected): release Monitor Nov25.16, Monitor `V`, `Tracker ID: T2` (do not re-pair). **Always check the S21FE's foreground app right before every tap.** With both connected, never `gradlew installDebug`: `adb -s <serial> install -r`, or `TESTS\install_release.ps1 -Serial <serial> -Apps monitor`.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). Sit data: stream `adb logcat -v time -s REV_Motion:* REV_Event:* REV_SitLearning:*` to a PC file (cmd redirection).

## 🟢 Status: PASSED (Nov26.25)
- ISSUE-NOV26.0-O163 part 2: the learning round (`SitLearning.kt`, `MotionSentinel.onSitCheck`, `SitLearnWire.kt`, `SensorService`, `SensorDataStore`, `SeatPlacementComponents.kt`); chapter 260.
- All unit tests, assembleDebug and assembleRelease; no device step (the user away, the relay down) and no device audit (no start-up or first-screen change).

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: O163 part 3 and chapters 258.3, 259.2 on a device; item 3: the relay's return, then 260.2).

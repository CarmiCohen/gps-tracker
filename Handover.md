# Project Handover - Version Nov27.4 (Own Trail Tests)

## ▶️ Resumption State (Read First)
- **Git**: Nov27.4 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `:core-shared:animalsnifferMain` fails the build on a JDK call Android 26 lacks. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; SIMP-NOV9.3-S1 low, best after O72; O140 very low. 2026-11-26: vibration 4 hits in 10 s; noise 2 s within 3 s; MOVING by GPS only; device tests one step at a time, answered by `//`. Nov26.3: the sitting period rules. O163 is done in parts; part 4, Feedback, is left.
- **Wire**: install both apps at Nov23.4 or later together. `sensor_behaviour` carries `placeId`, `placeName`, `placeType` (Nov26.4); `sit_learn` and `sit_examples` since Nov26.25, ignored by older apps; Nov26.26 adds `occupied`. The Monitor derives the Sensor's MOVING and its drives from its rows; no wire change.
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard`): debug Sensor with the Nov26.3 code running as `T`, under the chair cushion; no Monitor. S21FE (`R5CRC14PG4F`): release Monitor Nov27.2, Monitor `V`, `Tracker ID: T2` (do not re-pair); placements: `Chair` only, in use. **Always check the S21FE's foreground app right before every tap, and the screen's title after every navigation tap.** With both connected, never `gradlew installDebug` unpinned: `adb -s <serial> install -r`, or `TESTS\install_release.ps1 -Serial <serial> -Apps monitor`. The map's SAVE writes all trail rows to `/sdcard/Download/GPS-Tracker/`.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). Sit data: stream `adb logcat -v time -s REV_Motion:* REV_Event:* REV_SitLearning:*` to a PC file (cmd redirection).
- **Queue order** (2026-11-27): O169, O168; O166 after O133.

## 🟢 Status: PASSED (Nov27.4)
- ISSUE-NOV27.2-O170 follow-up (why the tests missed it, user 2026-11-27): instrumented `OwnTrailRecordingTest` runs the real `OwnTrailRecorder` to Room path (4 OK on the A15); `LocalTrailTest` removed (it tested only itself; user delegated the decision); new device chapter 264 for the background rate, pending (needs a 15-min walk with the S21FE).
- Nov27.3 (O167, the map CLEAR's kept span) before it in this session. Tests: 1309 unit tests (app-monitor counts twice, debug and release), both builds. No device audit; the S21FE still runs release Monitor Nov27.2.

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: O163 part 4 Feedback after 254.3's car recording (the user in the parked car with the A15), chapter 258.3 on a device; with no user at hand, O169 then O168, or O157, O159, O160; item 3: the relay's return, then 260.2, 261.2, 262.2 and 263.2).

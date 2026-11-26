# Project Handover - Version Nov26.1 (Sit Data)

## ▶️ Resumption State (Read First)
- **Git**: Nov26.1 (docs and test data only) committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `:core-shared:animalsnifferMain` fails the build on a JDK call Android 26 lacks. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; SIMP-NOV9.3-S1 low, best after O72; O140 very low. 2026-11-26: vibration 4 hits in 10 s; noise 2 s within 3 s; MOVING by GPS only. Device tests one step at a time, answered by `//`. 2026-11-26 (Nov26.1): O155 becomes a sitting period (start alerts, jolts and re-sits inside it do not, the end is an INFO event; a farmer on a tractor); then O163 (row 96).
- **Wire**: install both apps at Nov23.4 or later together. The Sensor sends `SIT_ALERT` since Nov25.23. The Monitor derives the Sensor's MOVING from its rows (`MovementTracker`).
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard`): debug Sensor Nov25.25 code (name Nov25.24) running as `T`, under the chair cushion (fixed position since 11:02 on 2026-11-26); no Monitor. S21FE (`R5CRC14PG4F`, not connected): release Monitor Nov25.16, Monitor `V`, `Tracker ID: T2` (do not re-pair). **Always check the S21FE's foreground app right before every tap.** With both connected, never `gradlew installDebug`: `adb -s <serial> install -r`, or `TESTS\install_release.ps1 -Serial <serial> -Apps monitor`.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). Sit data: stream `adb logcat -v time -s REV_Motion:* REV_Event:*` to a PC file (cmd redirection); per-minute peaks from `run-as com.jd.app.sensor` in `/data/user_de/0/com.jd.app.sensor/files/graph_history_sensor.jsonl` (a row is stamped at its minute's end).

## 🟢 Status: PASSED (Nov26.1)
- ISSUE-NOV25.23-O155: chair measuring session on the A15 (log `TESTS/data/Nov26.1_chair_session.log`); results, findings and the user's new start-and-end requirement in row 88. Breathing not measured (user decision: no code change this session).
- `:core-shared:test` only (D5); no builds, no device audit.

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: O155's implementation, `/effort high`, then O163; item 3: the relay's return).

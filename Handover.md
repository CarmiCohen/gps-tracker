# Project Handover - Version Nov25.9 docs (A15 Live Checks)

## ▶️ Resumption State (Read First)
- **Git**: Nov25.9 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73; still 503 on 2026-11-25): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. Since Nov25.5 `:core-shared:animalsnifferMain` (API 26 signature) runs before its jar and tests: a JDK call Android 26 lacks fails the build. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17 (no test removal without confirmation); Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O134 to discuss; SIMP-NOV19.5-S1 deferred; SIMP-NOV22.8-S1, S2 low (2026-11-25); SIMP-NOV9.3-S1 (R8) low; SIMP-NOV7.17-S1 low. 2026-11-23: lean workflow; small related items may share a version; effort recommended once per item, no stop.
- **Wire**: install both apps at Nov23.4 or later together. Since Nov23.9 the service start, stop and Home Points events carry a kind (`k`). Since Nov23.11 a violation travels only as its critical row. Since Nov23.13 the Monitor's settings file writes the siren type only inside `alertSettings`.
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard` unlocks it): debug Sensor Nov25.4 running as `T`, no Monitor, battery simulation reset. S21FE (`R5CRC14PG4F`): release Monitor Nov25.4, Monitor `V` paired to Tracker ID `tracker ` (not `T`: the user's responsibility, user decision 2026-11-25, do not re-pair). Nov25.8 was not installed on either.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). The Sensor's event file is in device-protected storage: `/data/user_de/0/com.jd.app.sensor/files/`. A noise alert needs about 2 s of steady loud sound, so a short shout is not enough; the per-minute peaks are in `graph_history_sensor.jsonl`. Screenshots: `adb -s <serial> shell screencap` then `pull` to a new file name each time.

## 🟢 Status: PASSED (Nov25.9 docs)
- Focus item 2 on the A15, Sensor side (relay down): 241.4, 237.3 (torch), 211.2 and 213.2's Active apps tap passed; status lines updated in `TESTS/TEST_PROCEDURE.md`.
- Not run: 237.3's light-off-on-the-Monitor part (needs the relay) and 213.2's Monitor Active apps tap (the S21FE was not connected); both moved to focus items 3 and 4.
- Docs only (D5): `:core-shared:test`; no builds, no device audit.

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: the user names the next no-phone item).

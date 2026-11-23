# Project Handover - Version Nov23.13 (One Siren Type Setting)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.13 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17 (no test removal without confirmation); Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O134 to discuss; SIMP-NOV19.5-S1 deferred; SIMP-NOV22.8-S1, S2 very low; SIMP-NOV9.3-S1 (R8) low. 2026-11-23: lean workflow; small related items may share a version; effort recommended once per item, no stop.
- **Wire**: install both apps at Nov23.4 or later together. Since Nov23.9 the service start, stop and Home Points events carry a kind (`k`). Since Nov23.11 a violation travels only as its critical row. Since Nov23.13 the Monitor's settings file writes the siren type only inside `alertSettings`.
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard` unlocks it): debug Sensor Nov23.10 running as `T`, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). The Sensor's event file is in device-protected storage: `/data/user_de/0/com.jd.app.sensor/files/`.

## 🟢 Status: PASSED (Nov23.13)
- SIMP-NOV7.3-S1: the Monitor's siren type lives only in `AlertSettings.sirenType`; a DataStore migration moves an installed phone's `siren_type` key into it, and an older settings file's top-level `sirenType` is moved into its `alertSettings` (the Sensor dropped its key in O82).
- 1141 unit tests passed (new `SirenTypeMigrationTest`, a settings-file test), debug and release built (only the known warnings, O72), Monitor instrumented sources compile.
- Device audit not needed: no change to start-up, services or the first screen; the migration is checked at the S21FE's next Monitor install (focus item 4).

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: SIMP-NOV7.2-S1, no phones needed).

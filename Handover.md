# Project Handover - Version Nov23.9 (Event Kinds)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.9 committed, tagged and pushed by Claude. Its commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73): after Render's reset (probably 2026-12-01) the user deploys relay v6.049 (`8b88b42`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17 (no test removal without confirmation); Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O134 to discuss; SIMP-NOV19.5-S1 deferred; SIMP-NOV22.8-S1, S2 very low; SIMP-NOV9.3-S1 (R8) low. 2026-11-23: lean workflow; small related items may share a version; effort recommended once per item, no stop.
- **Wire**: install both apps at Nov23.4 or later together. Nov23.9 sends the service start, stop and Home Points events with a kind (`k`); an older Monitor still reads their wording.
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard` unlocks it): debug Sensor Nov23.9 running as `T`, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). The Sensor's event file is in device-protected storage: `/data/user_de/0/com.jd.app.sensor/files/`.

## 🟢 Status: PASSED (Nov23.9)
- SIMP-NOV23.4-S1: the service start, stop and Home Points events carry an `EventEntry.kind`; the Graphs, `forPeer` and the unrecorded-stop check read it, with the message prefix as fallback for events without a kind.
- 1125 unit tests passed, debug and release built; A15: `ProductionReadinessAuditTest` OK, release cold start 0 Davey, 0 StackLog; the stored events show the new kinds.

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: SIMP-NOV8.103-S1, no phones needed).

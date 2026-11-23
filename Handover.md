# Project Handover - Version Nov23.7 (One Status Place)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.7 committed, tagged and pushed by Claude. Its commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73): after Render's reset (probably 2026-12-01) the user deploys relay v6.049 (`8b88b42`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background.
- **User decisions**: Rule 17 (no test removal without confirmation); Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O134 to discuss; SIMP-NOV19.5-S1 deferred; SIMP-NOV22.8-S1, S2 very low. 2026-11-23: lean workflow; small related items may share a version; effort recommended once per item, no stop; Nov23.7's removal of 6 status-text tests confirmed.
- **Wire**: install both apps at Nov23.4 or later together (unknown battery as null from Nov23.1, silence causes from Nov23.2, event kinds from Nov23.4). Nov23.7 changes no wire field.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov23.7 running as `T`, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes).

## 🟢 Status: PASSED (Nov23.7)
- SIMP-NOV15.5-S1: the Sensor Setup dialog shows only IDs, relay URL and version (+ `INFO`, `SETTINGS`); console without microphone and motion lines; Monitor Setup without its relay line. Info is each state's one place.
- `run_tests.ps1`: 1120 tests passed, debug, release and androidTest builds; A15 audit clean (`ProductionReadinessAuditTest` OK, 0 Davey > 500 ms, 0 StackLog).
- The new dialog was not looked at on the phone (the A15 was locked).

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: SIMP-NOV9.3-S1, no phones needed).

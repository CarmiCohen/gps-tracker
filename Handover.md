# Project Handover - Version Nov25.1 (One Fix Freshness Rule)

## ▶️ Resumption State (Read First)
- **Git**: Nov25.1 committed, tagged and pushed by Claude; relay v6.050 (`c460e25`) pushed earlier. The commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73): after Render's reset (probably 2026-12-01) the user deploys relay v6.050 (`c460e25`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17 (no test removal without confirmation); Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O134 to discuss; SIMP-NOV19.5-S1 deferred; SIMP-NOV22.8-S1, S2 very low; SIMP-NOV9.3-S1 (R8) low; SIMP-NOV7.17-S1 low. 2026-11-23: lean workflow; small related items may share a version; effort recommended once per item, no stop.
- **Wire**: install both apps at Nov23.4 or later together. Since Nov23.9 the service start, stop and Home Points events carry a kind (`k`). Since Nov23.11 a violation travels only as its critical row. Since Nov23.13 the Monitor's settings file writes the siren type only inside `alertSettings`.
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard` unlocks it): debug Sensor Nov25.0 running as `T`, no Monitor (Nov25.1 not installed: no device check needed). S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes). The Sensor's event file is in device-protected storage: `/data/user_de/0/com.jd.app.sensor/files/`.

## 🟢 Status: PASSED (Nov25.1)
- SIMP-NOV7.21-S1: one `FIX_CURRENT_MS` (15 s) and `isFixFresh` in `core-ui`; the Sensor's telemetry (`SensorFixStore.currentLocation`), HUD speed and GPS LED use it; the Monitor's own speed keeps 35 s (`MONITOR_SPEED_VALID_MS`). `ForensicBuffer.writeTrace` reads `update.isCritical`.
- 1145 unit tests passed, debug and release built, Sensor androidTest compiled.
- Not run on a phone: no start-up, service-flow or first-screen change (the HUD speed now drops at 15 s instead of 35 s).

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: SIMP-NOV7.15-S1, no phones needed).

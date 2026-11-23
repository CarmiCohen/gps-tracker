# Project Handover - Version Nov23.8 (Release Starts Checked)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.8 committed, tagged and pushed by Claude. Its commit message is the record (D1); the archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4.
- **Relay**: SUSPENDED (O133, row 73): after Render's reset (probably 2026-12-01) the user deploys relay v6.049 (`8b88b42`); the waiting live checks are listed in `issues.md` focus item 3.
- **PC**: if Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background.
- **User decisions**: Rule 17 (no test removal without confirmation); Claude pushes the main and relay repos; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O134 to discuss; SIMP-NOV19.5-S1 deferred; SIMP-NOV22.8-S1, S2 very low; SIMP-NOV9.3-S1 (R8) low (2026-11-23). 2026-11-23: lean workflow; small related items may share a version; effort recommended once per item, no stop.
- **Wire**: install both apps at Nov23.4 or later together (unknown battery as null from Nov23.1, silence causes from Nov23.2, event kinds from Nov23.4). Nov23.8 changes no code.
- **Phones**: A15 (`R58X40GV2AR`, no screen lock: `adb shell wm dismiss-keyguard` unlocks it): debug Sensor Nov23.7 running as `T`, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with each app's `HiltTestRunner` (Operational Notes).

## 🟢 Status: PASSED (Nov23.8)
- SIMP-NOV9.3-S1: both release APKs already ship `assets/dexopt/baseline.prof` and `profileinstaller` 1.3.0 (through Compose and Activity); the idea now holds only R8, low priority.
- A15 check of Nov23.7: Setup dialog shows only IDs, relay URL, version, `INFO` (opens Sensor Info) and `SETTINGS`; console has no microphone or motion lines.
- Docs-only version (D5): `:core-shared:test` only; no builds, no device audit.

## 🎯 Next Resumption Focus
See `DOCS/issues.md`, Next Resumption Focus (item 1: SIMP-NOV23.4-S1, no phones needed).

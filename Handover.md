```
git add .
git commit -m "Nov16.1 - THE SETTINGS FILE (ISSUE-NOV10.8-O80 part 1)

- O80: Export/Import of a JSON settings file in both apps (Maintenance screen; Monitor also on its pairing screen), via the system file picker.
- Monitor file: pairing, relay, alert and siren settings, home points and distance, all trails; imported settings get new revisions for the Sensor.
- Sensor file: ID, relay, detection settings, geofence, next sequence; the import sets a sequence floor (O16 avoided on restore).
- Index row Nov16.0 filled (7d677bf).
- TESTS: unit 823 PASSED; A15 live export/uninstall/reinstall/import; ProductionReadinessAuditTest + SensorHudValuesTest 8/8; release audits clean.
- VERSION: Nov16.1, versionCode 107."
git tag Nov16.1
git push origin main --tags
```

# Project Handover - Version Nov16.1 (The Settings File)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov16.1^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.1 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue". O80 split (2026-11-16): part 1 settings file done, part 2 store move + Auto Backup next.
- **Phones**: A15 (`R58X40GV2AR`): Nov16.1 debug Sensor `T` and Monitor `V`, paired; both apps were uninstalled and their data restored by import (60319 trail rows, 0 home points); permissions granted again by adb; Sensor test APK updated, Monitor test APK from Nov16.0; airplane mode off; the two test files stay in Downloads (`GPS-*-settings-20261116-*.json`). S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; chapter 194.3 for the live settings file check (taps found by `uiautomator dump`; the picker is in Hebrew on the A15; the events files are read with `run-as ... cat files/event_log.jsonl`; PowerShell 5 `ConvertFrom-Json` fails on the 5.7 MB Monitor file).

## 🟢 Status: PASSED (Nov16.1)
- ISSUE-NOV10.8-O80 part 1 (the settings file) done; O80 stays open for part 2; O16 text updated; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: PASSED (823); `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 194.3 live; `ProductionReadinessAuditTest` + `SensorHudValuesTest` 8/8; release cold-start audits clean.
- Not run: the full instrumented suites (the Monitor's UI changes are a new button on the pairing screen and Maintenance screen, checked live); stress protocol; chapters marked H or S; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.1: [SOT Count: 1007 (Rules: 507), Open: H:0, M:4, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 390), QA: 129]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O80 part 2: move the Sensor and identity stores out of `noBackupFilesDir`, then Auto Backup rules (`/effort high`).
4. O98, O99, O100, O86, O94 to O97 when convenient.

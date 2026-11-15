```
git add .
git commit -m "Nov15.2 - THE MONITOR SETS THE SENSOR'S DETECTION (ISSUE-NOV10.8-O83 part 1)

- O83: tilt, vibration, noise switches and limits are set on the Monitor and sent as a revisioned sensor_behaviour message (relay unchanged); the Sensor shows them read-only.
- A switched-off kind is not detected; noise off closes the microphone; new Noise Sensitivity slider (45/30/15 dB).
- Old Sensor slider values kept only until the Monitor's first message (LegacyAlertSettingsMigration).
- New gap O97 (small leftovers). Index row Nov15.0 filled (9d23d72).
- TESTS: unit 661; A15 live delivery 0.3 s, mic closed/reopened; ProductionReadinessAuditTest 3/3; release audits clean.
- VERSION: Nov15.2, versionCode 101."
git tag Nov15.2
git push origin main --tags
```

# Project Handover - Version Nov15.2 (The Monitor Sets The Sensor's Detection)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov15.2^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov15.2 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue". O83 decisions of 2026-11-15 are in the archive and in `issues.md` (part 2 design).
- **Phones**: A15 (`R58X40GV2AR`): Nov15.2 debug Sensor `T` and Monitor `V` paired; all four sensor switches on, sliders 0.5 (Sensor at rev from 2026-11-15); Sensor test APK installed. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, paired `V` to `T`, Global Mute on (an old Monitor sends no detection settings; the Sensor keeps the newest it has).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; the Sensor test runner is `com.jd.app.sensor.test/com.jd.app.sensor.HiltTestRunner`; airplane mode on for `am instrument` (O90).

## 🟢 Status: PASSED (Nov15.2)
- ISSUE-NOV10.8-O83 part 1 closed; part 2 (Sensor's setup status report and Monitor warnings) stays open as item 68. New Low gap O97. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 661 passed; `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 188.4 live (sent, stored, applied in 0.3 s; microphone closed and reopened); `ProductionReadinessAuditTest` 3/3; release cold-start audits clean.
- Not run: the full instrumented suites (no instrumented test changed besides a fake); chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov15.2: [SOT Count: 1001 (Rules: 501), Open: H:0, M:6, L:18, Ideas: H:0, M:0, L:21, Testing: PASSED (Sub-items: 366), QA: 123]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O83 part 2: the Sensor reports its Phone Setup status, the Monitor warns (`/effort high`).
4. Then O84 (three versions), O85, O80; O94, O95, O96, O97 when convenient.

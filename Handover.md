```
git add .
git commit -m "Nov9.1-MicOnStartup - THE SENSOR SURVIVES A REBOOT (ISSUE-NOV8.103-O60)

- Found: after a reboot the Sensor did not run at all (location and microphone types refused, undeclared specialUse fallback). It now keeps running with the types Android allows.
- Tap-to-enable notification ('Tracking off' / 'Noise detection off - tap to enable') restores location and microphone without opening the app.
- Monitor alarm 'Tracker noise detection off' from a new noiseOff telemetry field; Sensor asks for 'Allow all the time' location.
- Index row Nov9.0-LiftTilt filled (b1f1d38); Group 7 decided (build). New gap ISSUE-NOV9.1-O61, idea SIMP-NOV9.1-S1.
- TESTS: 475 passed (test assembleDebug assembleRelease); A15 three reboots, ProductionReadinessAuditTest 3/3, device audit clean (Monitor first frame = O55); chapter 163.
- VERSION: Nov9.1-MicOnStartup, versionCode 71."
git tag Nov9.1-MicOnStartup
git push origin main --tags
```

# Project Handover - Version Nov9.1-MicOnStartup (The Sensor Survives A Reboot)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov9.1-MicOnStartup^{commit}`.
- **User decisions (2026-11-09)**: O60 widened to "the Sensor survives a reboot"; the Monitor warning is a normal alarm for every reason the microphone is off ("keep"); the Sensor asks for "Allow all the time" location. ISSUE-NOV5.2-O11 Group 7: build the `Tamper / Vibration Sensor` switch. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`) has the Nov9.1 code as debug builds (built before the version bump, so they show `Nov9.0-LiftTilt`); Sensor `T` running with location and microphone (`0x289`), now with "Allow all the time" location; Monitor paired to `T`, in front, no pending alarm. S21FE in personal use; never test on it.
- **Test tips**: `DOCS/issues.md` Operational Notes (reboot test on the A15: 1.5 min boot broadcast, two taps on the Samsung lock screen); `TESTS/TEST_GUIDELINES.md` section 4. The Sensor test runner is `com.jd.app.sensor.HiltTestRunner`.

## 🟢 Status: PASSED (Nov9.1-MicOnStartup)
- ISSUE-NOV8.103-O60 resolved and widened: the Sensor runs after a reboot, one notification tap restores location and microphone, the Monitor raises `Tracker noise detection off`. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 475 tests passed with debug and release builds, no new warning. A15: three reboots (without and with "Allow all the time", with the Monitor open), `ProductionReadinessAuditTest` 3/3, device audit Sensor clean; Monitor cold start 1 Davey frame of 722 ms, the known ISSUE-NOV8.13-O55.
- **Not run**: chapters 161.4 (clap) and 162.4 (tilt and pickup) still need a person at the A15.

**Audit line**: Nov9.1-MicOnStartup: [SOT Count: 975 (Rules: 475), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:20, Testing: PASSED (Sub-items: 311), QA: 97]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. ISSUE-NOV5.2-O11 Group 7: build the `Tamper / Vibration Sensor` switch (`/effort high`).
3. Next to the A15: chapters 161.4 (clap) and 162.4 (tip it over; rest 30 s; pick it up 30 cm) (`/effort low`).
4. Then the Work Queue in `DOCS/issues.md`.

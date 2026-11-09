```
git add .
git commit -m "Nov9.0-LiftTilt - SENSOR TILT AND LIFT ALARMS (ISSUE-NOV5.2-O11 GROUP 5)

- Sensor detects tilt and lift of the resting tracker (accelerometer on every phone, barometer where present; arms after 30 s rest); Monitor raises 'Tracker tilted' / 'Tracker lifted'. Tilt angle from the Sensor's own slider.
- Fixed: on phones without a gravity sensor (A15) the vibration value was always 0; the accelerometer gravity estimate now feeds it.
- One reportViolation helper and one alert queue in the sensing loop; debug STATE adds mic/motionArmed/barometer.
- Nov8.103-Microphone index row: tag commit e9dd12e (c7f34b8 is a later untagged commit). New idea SIMP-NOV9.0-S1.
- TESTS: 322 passed (test assembleDebug assembleRelease); A15 armed in 30 s, no false alert in 5 min, ProductionReadinessAuditTest 3/3, device audit clean; chapter 162 (162.4 pending).
- VERSION: Nov9.0-LiftTilt, versionCode 70."
git tag Nov9.0-LiftTilt
git push origin main --tags
```

# Project Handover - Version Nov9.0-LiftTilt (Sensor Tilt And Lift Alarms)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov9.0-LiftTilt^{commit}` (the tag's commit, not HEAD).
- **User decisions (2026-11-09)**: lift by accelerometer pickup on every phone plus the barometer where present; the Sensor uses its own tilt slider (the Monitor shows a note instead); the gravity defect was fixed here. Earlier: only the Sensor uses a microphone; Monitor switches only gate alarms. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`) has the Nov9.0 code as debug builds (built before the version bump, so they show `Nov8.103-Microphone`); Sensor `T`, `mic=LISTENING`, tilt and lift armed when at rest; Monitor paired to `T` with its 2 Home Points, in front. The A15 has no barometer, gyroscope, or gravity sensor. S21FE in personal use; never test on it.
- **Test tips**: `TESTS/TEST_GUIDELINES.md` section 4; `device_check.ps1 -App sensor -Command STATE` shows `mic=` and `motionArmed=`. `am instrument` force-stops the Sensor; one tap on its `System Idle` screen restarts it.

## 🟢 Status: PASSED (Nov9.0-LiftTilt)
- ISSUE-NOV5.2-O11 Group 5 built: tilt and lift on the Sensor, `Tracker tilted` / `Tracker lifted` on the Monitor; A15 vibration value fixed. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 322 tests passed with debug and release builds, no new warning. A15: armed 30 s after start, no false alert over 5 minutes at rest, `ProductionReadinessAuditTest` 3/3, device audit clean in both apps.
- **Not run**: a real tilt and pickup (chapter 162.4) and the clap (161.4): the user was away; the barometer on a real phone (the A15 has none).

**Audit line**: Nov9.0-LiftTilt: [SOT Count: 974 (Rules: 474), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:19, Testing: PASSED (Sub-items: 307), QA: 96]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Next to the A15: chapters 161.4 (clap) and 162.4 (tip it over; rest 30 s; pick it up 30 cm) (`/effort low`).
3. Decide ISSUE-NOV5.2-O11 Group 7 (the `Tamper / Vibration Sensor` switch: build or remove) and ISSUE-NOV8.103-O60 (microphone after a reboot).
4. Then the Work Queue in `DOCS/issues.md`.

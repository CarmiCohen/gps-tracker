```
git add .
git commit -m "Nov8.103-Microphone - SENSOR MICROPHONE NOISE ALARM (ISSUE-NOV5.2-O11 GROUP 4)

- Sensor listens (AudioRecord, microphone service type, RECORD_AUDIO optional) and reports a 30 dB jump above the background as ACOUSTIC_ALERT; Monitor raises 'Tracker noise detected' (switch acousticAlert). Monitor never opens a microphone.
- Sensor screen shows the microphone state (Audio Denied, Listening, Open the Sensor app...); opening the app takes the microphone after a background start.
- New open gap ISSUE-NOV8.103-O60 (no noise detection after a reboot until the app is opened); new idea SIMP-NOV8.103-S1. Nov8.102-Documentation index row filled (733747b).
- TESTS: 221 passed (run_tests.ps1 test assembleDebug assembleRelease); A15 ProductionReadinessAuditTest 3/3, device audit clean; test chapter 161 (161.4 real noise pending).
- VERSION: Nov8.103-Microphone, versionCode 69."
git tag Nov8.103-Microphone
git push origin main --tags
```

# Project Handover - Version Nov8.103-Microphone (Sensor Microphone Noise Alarm)

## ▶️ Resumption State (Read First)
- **Git**: run the block above; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` (Resumption step 1).
- **User decisions (2026-11-08)**: only the Sensor uses a microphone and sends events, the Monitor never opens one; the Monitor switch only gates the alarm; trigger = 30 dB jump above the learned background. A message of just `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`) has the Nov8.103-Microphone code as debug builds (built before the version bump, so they show `Nov8.102-Documentation`); Sensor ID `T`, listening; Monitor paired to `T` with its 2 Home Points, in front. The Sensor's location, notification, and microphone permissions are granted (location and notifications were found off and granted with `pm grant`). S21FE in personal use; never test on it.
- **Test tips**: `TESTS/TEST_GUIDELINES.md` section 4. `am instrument` force-stops the Sensor; one tap on its `System Idle` screen restarts it with the microphone.

## 🟢 Status: PASSED (Nov8.103-Microphone)
- ISSUE-NOV5.2-O11 Group 4 built: Sensor noise detection, Monitor alarm `Tracker noise detected`. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 221 tests passed with debug and release builds, no new warning. A15: `ProductionReadinessAuditTest` 3/3, device audit 0 Davey, StackLog, or fatal in both apps; `Audio Denied` and `Listening` seen live.
- **Not run**: a real noise next to the A15 (chapter 161.4); the user was away.

**Audit line**: Nov8.103-Microphone: [SOT Count: 973 (Rules: 473), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:18, Testing: PASSED (Sub-items: 303), QA: 93]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row (after the Git block).
2. Next to the A15: chapter 161.4, clap once near the phone; expect `Tracker noise detected` on the Monitor.
3. ISSUE-NOV5.2-O11 Group 5 (Lift and Tilt), High, as version **Nov9.0-LiftTilt** (user's name); `/effort high`.
4. ISSUE-NOV8.103-O60: decide on the after-reboot microphone (accept, or a notification).

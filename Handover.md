```
git add .
git commit -m "Nov9.4-OwnVibration - THE SENSOR PHONE'S OWN RINGING IS NO KNOCK (ISSUE-NOV9.2-O62, ISSUE-NOV5.2-O11)

- O62: a call shook the A15 by 0.11 g; vibration is now ignored while the phone rings or notifies (OwnVibrationWatch), held 200 ms, 1 s tail.
- 164.4: a table knock (0.15 g) never alerted at 0.5 g; the default limit is now 0.13 g (0.82 g at 0.1, 0.10 g at 1.0).
- O11 closed: person checks 161.4, 162.4, 164.4 run on the A15. New gaps O65 to O69.
- Index row Nov9.3-MonitorStart filled (5569bcf). SOT 1.404, chapter 166, OwnVibrationWatchTest.
- TESTS: 547 unit passed, ProductionReadinessAuditTest 3/3, call and knock on the A15, release audit clean.
- VERSION: Nov9.4-OwnVibration, versionCode 74."
git tag Nov9.4-OwnVibration
git push origin main --tags
```

# Project Handover - Version Nov9.4-OwnVibration (The Sensor Phone's Own Ringing Is No Knock)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov9.4-OwnVibration^{commit}`.
- **User decisions (2026-11-09)**: ignore vibration while the Sensor phone rings or notifies, and lower the default limit to about 0.13 g. Version name chosen by Claude (the user gave none). `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`) has the Nov9.4 debug builds; Monitor `V` paired to `T`, 2 Home Points, radius 140; Sensor slider 0.5 (0.13 g). The Sensor service needs one tap on `System Idle` after the last install. S21FE in personal use; never test on it.
- **Test tips**: `DOCS/issues.md` Operational Notes (with both apps on one phone, the Monitor siren pauses vibration detection; wait for the siren to stop); `TESTS/TEST_GUIDELINES.md` section 4. Rebuild the release APKs after a version bump before `install_release.ps1`, or Android refuses a downgrade.

## 🟢 Status: PASSED (Nov9.4-OwnVibration)
- ISSUE-NOV9.2-O62 fixed, ISSUE-NOV5.2-O11 closed (all person checks run); new gaps ISSUE-NOV9.4-O65 to O69. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 547 unit test results, 0 failures; builds clean apart from the known JVM and kapt lines. `ProductionReadinessAuditTest` 3/3 on the A15.
- On the A15 with a person: a 15 s call gave no vibration alert (vibration stayed armed); a hard table knock gave 0.46 g against the 0.13 g limit.
- Release audit: Sensor 375 ms, Monitor 470 ms, 0 Davey, StackLog, fatal. All instrumented suites were not re-run (only the audit test); the change is unit-tested logic and a device-checked watch.

**Audit line**: Nov9.4-OwnVibration: [SOT Count: 978 (Rules: 478), Open: H:0, M:2, L:16, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 321), QA: 100]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 46, ISSUE-NOV8.13-O56: a decision on unique Sensor IDs (`/effort medium`).
3. Item 55, ISSUE-NOV9.3-O64: a small test fix (`/effort low`).
4. Item 56, ISSUE-NOV9.4-O66: pause the noise alarm while the phone rings, reusing `OwnVibrationWatch` (`/effort medium`).
5. Then the Work Queue in `DOCS/issues.md`.

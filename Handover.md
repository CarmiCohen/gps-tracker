```
git add .
git commit -m "Nov12.5 - THE SENSOR'S OWN PHONE SETUP (ISSUE-NOV10.8-O81)

- O81 closed: Sensor-only Phone Setup, 8 maker/Android-specific items with live status.
- O81: console's 3 grant buttons -> one PHONE SETUP: N MISSING line; REQUEST_IGNORE_BATTERY_OPTIMIZATIONS added (battery dialog was dead).
- New Low gap O91 (English text mirrored on an RTL phone). Chapter 184.2 checked on the A15.
- Index row Nov12.4 filled (40c0278).
- TESTS: run_tests 552 passed; A15 audit 3/3; release audit clean; chapter 185.
- VERSION: Nov12.5, versionCode 97."
git tag Nov12.5
git push origin main --tags
```

# Project Handover - Version Nov12.5 (The Sensor's Own Phone Setup)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.5^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`, not redeployed since Nov11.2-relay; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue". 2026-11-12: the Sensor has no Map button; its red X only goes to the background; the Sensor's Phone Setup item list (O81, archive).
- **Phones**: A15 (`R58X40GV2AR`): Nov12.5 debug builds, Sensor `T` service running and now battery-exempt (Phone Setup item 5), 'Pause app activity if unused' still on (item 6), Monitor `V` paired to `T`, airplane mode off. S21FE (`R5CRC14PG4F`): the user's state, release Monitor Nov12.1, unpaired, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (`connectedDebugAndroidTest` uninstalls the app: prefer `am instrument`; restore steps there); `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov12.5)
- ISSUE-NOV10.8-O81 closed: the Sensor has its own Phone Setup with live status; the console shows the missing count. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Found and fixed: the battery request dialog never opened (missing manifest permission).
- Chapter 184.2 passed on the A15; chapter 182.2 still waits for the next Manual Deploy.

**Audit line**: Nov12.5: [SOT Count: 998 (Rules: 498), Open: H:0, M:6, L:14, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 357), QA: 120]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O83: Sensor behaviour settings move to the Monitor by relay, Sensor reports its setup status (reuse `SensorSetupStatus`) (`/effort high`).
4. Then O84 (three versions), O85, O80; O90 and O91 (RTL, ask the user) when convenient.

```
git add .
git commit -m "Nov12.4 - THE SENSOR HAS NO SIREN PAGE (ISSUE-NOV10.8-O82)

- O82 closed: Sensor SIREN SETUP button and page removed; siren type, volume setters and DataStore key gone.
- O82: RemoveSirenTypeMigration deletes a stored siren_type once; AlarmSoundOverlay is Monitor-only (no isTracker).
- Index row Nov12.3 filled (0a334a8).
- TESTS: run_tests 532 passed; A15 audit 3/3; release audit clean; chapter 184.
- VERSION: Nov12.4, versionCode 96."
git tag Nov12.4
git push origin main --tags
```

# Project Handover - Version Nov12.4 (The Sensor Has No Siren Page)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.4^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`, not redeployed since Nov11.2-relay; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue". 2026-11-12: the Sensor has no Map button; its red X only goes to the background.
- **Phones**: A15 (`R58X40GV2AR`): Nov12.4 debug builds, Sensor `T` service running, Monitor `V` paired to `T`, airplane mode off. S21FE (`R5CRC14PG4F`): the user's state, release Monitor Nov12.1, unpaired, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (airplane mode for Monitor `am instrument` runs, O90); `TESTS/TEST_GUIDELINES.md` section 4. `device_audit.cmd` needs `<serial> <package>` and a running app.

## 🟢 Status: PASSED (Nov12.4)
- ISSUE-NOV10.8-O82 closed: the Sensor no longer shows or stores siren settings. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Not run: chapter 184.2 (look at the Sensor's Settings page on the A15); no UI test exists for the Sensor's Hilt activity.
- Chapter 182.2 still waits for the next Manual Deploy.

**Audit line**: Nov12.4: [SOT Count: 997 (Rules: 497), Open: H:0, M:7, L:13, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 354), QA: 119]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O81, the Sensor's Phone Setup (`/effort medium`); check chapter 184.2 on the A15 on the way.
4. Then O83 (with item 71's O83 warnings by push), O84 (three versions), O85, O80; O90 when convenient.

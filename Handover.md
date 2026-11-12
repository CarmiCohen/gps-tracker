```
git add .
git commit -m "Nov12.3 - NO BUTTON THAT DOES NOTHING (ISSUE-NOV10.8-O76, ISSUE-NOV12.3-O90)

- O76 closed: Tools overlay deleted; Events, Graphs hidden until O84; Sensor Map removed; red X to background in both apps, unpair only behind the Monitor gear.
- O76: Phone Setup lost Exact Alarms, Refresh Status, Diagnostics; Monitor Open Map opens the home point editor; setup pages show only actions given.
- O90 new: an alarm push crashes Monitor instrumented runs (airplane mode work-around).
- Index row Nov12.2 filled (4aa2453).
- TESTS: run_tests 494 passed; A15 am instrument 12/12 + audit 3/3; release audit clean; chapter 183.
- VERSION: Nov12.3, versionCode 95."
git tag Nov12.3
git push origin main --tags
```

# Project Handover - Version Nov12.3 (No Button That Does Nothing)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.3^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`, not redeployed since Nov11.2-relay; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue". 2026-11-12: the Sensor has no Map button; its red X only goes to the background.
- **Phones**: A15 (`R58X40GV2AR`): Nov12.3 debug builds, Sensor `T` running, Monitor `V` paired to `T`, airplane mode off. S21FE (`R5CRC14PG4F`): the user's state, release Monitor Nov12.1, unpaired, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (new: airplane mode for Monitor `am instrument` runs, O90); `TESTS/TEST_GUIDELINES.md` section 4. `device_audit.cmd` needs `<serial> <package>` and a running app.

## 🟢 Status: PASSED (Nov12.3)
- ISSUE-NOV10.8-O76 closed: no dead or misrouted button left in either app's top bar or setup pages. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- New Low issue ISSUE-NOV12.3-O90: a push crashes Monitor instrumented runs under `HiltTestApplication`.
- Unit test count 494 is below Nov11.1's 594; no unit test was removed in this version, cause not checked.
- Chapter 182.2 still waits for the next Manual Deploy.

**Audit line**: Nov12.3: [SOT Count: 996 (Rules: 496), Open: H:0, M:7, L:14, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 352), QA: 118]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O82, the Sensor's dead Siren page (`/effort medium`), then O81 (Sensor Phone Setup).
4. Then O83 (with item 71's O83 warnings by push), O84 (three versions), O85, O80; O90 when convenient.

```
git add .
git commit -m "Nov12.2 - THE SECOND VIEWER HEARS ALARMS ON TIME (ISSUE-NOV10.7-O73)

- O73 closed: chapter 180.5 on the S21FE, screen off 3 min: noise row by relay about 0.3 s (as the A15), push 131 ms later; session alive.
- Not run: a push starting a dead session (stays with O86).
- S21FE restored: Global Mute on, unpaired, release Monitor Nov12.1.
- Index row Nov12.1 filled (2d81d68).
- TESTS: :core-shared:test.
- VERSION: Nov12.2, versionCode 94."
git tag Nov12.2
git push origin main --tags
```

# Project Handover - Version Nov12.2 (The Second Viewer Hears Alarms On Time)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.2^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`, not redeployed since Nov11.2-relay; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov11.2-relay debug builds, Sensor `T` running, Monitor `V` paired to `T`. S21FE (`R5CRC14PG4F`): the user's state, now release Monitor Nov12.1 (versionCode 93), unpaired, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4. Rebuild the release before `install_release.ps1` if the phone holds a newer debug build (downgrade refused).

## 🟢 Status: PASSED (Nov12.2)
- ISSUE-NOV10.7-O73 closed: chapter 180.5 run on the S21FE with the screen off; no late alarm. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Not run: a push starting a dead session (no dead session without a force-stop; stays with O86); chapter 182.2 waits for the next Manual Deploy.
- Documentation only (D5): `:core-shared:test`.

**Audit line**: Nov12.2: [SOT Count: 995 (Rules: 495), Open: H:0, M:7, L:14, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 349), QA: 117]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O76, dead or misrouted buttons (`/effort medium`).
4. Then O82 and O81, O83 (with item 71's O83 warnings by push), O84 (three versions), O85, O80.

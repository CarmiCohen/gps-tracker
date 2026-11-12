```
git add .
git commit -m "Nov12.1 - THE RELAY'S DEPENDENCIES PINNED (ISSUE-NOV11.2-O89)

- O89 resolved: relay-server commits yarn.lock (relay commit c9dd901, pushed); Render keeps yarn install.
- PC on Node 24.22.1; relay npm test 15/15 on it. Chapter 182, SOT 1.421.
- Index row Nov12.0-push filled (edda875).
- TESTS: :core-shared:test.
- VERSION: Nov12.1, versionCode 93."
git tag Nov12.1
git push origin main --tags
```

# Project Handover - Version Nov12.1 (The Relay's Dependencies Pinned)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.1^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), code unchanged (v6.046, Node 24.x), now with `yarn.lock` (commit `c9dd901`, pushed); live on Render with `push: on`, not redeployed since; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1 (was 22.19). winget's `OpenJS.NodeJS.LTS` is Node 26 now: use `--version 24.22.1`, admin PowerShell. Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: unchanged this version. A15 (`R58X40GV2AR`): Nov11.2-relay debug builds, Sensor `T` running, Monitor `V` paired to `T`. S21FE (`R5CRC14PG4F`): the user's state (release Monitor Nov11.2-relay, unpaired, Global Mute on).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4; relay lockfile check: chapter 182.

## 🟢 Status: PASSED (Nov12.1)
- ISSUE-NOV11.2-O89 resolved: `yarn.lock` pins `firebase-admin` 13.10.0, `@firebase/database-compat` 2.1.8, `socket.io` 4.8.4. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Relay `npm test` 15/15 on Node 24.22.1; `--frozen-lockfile` up to date.
- Not run: Render build from the lockfile (chapter 182.2), waits for the user's next Manual Deploy. App code unchanged: `:core-shared:test` only (D5).

**Audit line**: Nov12.1: [SOT Count: 995 (Rules: 495), Open: H:0, M:8, L:14, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 349), QA: 117]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. When the S21FE is available (ask first): chapter 180.5 with a noise alarm while its screen is off, to close O73 (`/effort low`).
4. Then O76, O82 and O81, O83 (with item 71's O83 warnings by push), O84 (three versions), O85, O80.

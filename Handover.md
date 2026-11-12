```
git add .
git commit -m "Nov12.0-push - THE PUSH ON A SECOND PHONE (ISSUE-NOV10.7-O73, ISSUE-NOV10.8-O86)

- Documentation only: chapters 181.4 and 180.5 run on the S21FE; O73 and O86 stay open.
- 181.4: silence notice by push 35.4 s after the cut (screen off), one TRACKER_OFFLINE raise.
- 180.5: screen on, a noise row by push 120 ms after the relay; screen-off noise lag not measured yet.
- Operational Notes: how to test on the S21FE (pairing, mute, screen on at launch, RTL taps).
- Index row Nov11.2-relay filled (6b1162b).
- TESTS: :core-shared:test.
- VERSION: Nov12.0-push, versionCode 92."
git tag Nov12.0-push
git push origin main --tags
```

# Project Handover - Version Nov12.0-push (The Push On A Second Phone)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.0-push^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), unchanged (v6.046, Node 24.x), live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov11.2-relay debug builds, Sensor `T` running, Monitor `V` paired to `T`, Sensor network restored (chain3 disabled). S21FE (`R5CRC14PG4F`): restored to the user's state: release Monitor Nov11.2-relay, unpaired, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (new: S21FE Monitor test); `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov12.0-push)
- Documentation only (ISSUE-NOV10.7-O73, ISSUE-NOV10.8-O86, both open). Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 181.4 on the S21FE: push notice 35.4 s after the cut, one raise; the session stayed alive, so push and own count were 0.1 s apart and not told apart.
- 180.5: screen on only (push 120 ms after the relay); the screen-off noise lag is still unmeasured, so O73 is not confirmed fixed.
- `:core-shared:test` passed.

**Audit line**: Nov12.0-push: [SOT Count: 994 (Rules: 494), Open: H:0, M:8, L:15, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 347), QA: 116]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. ISSUE-NOV11.2-O89: pin the relay's dependencies with a lockfile (`/effort medium`).
3. When the S21FE is available (ask first): chapter 180.5 with a noise alarm while its screen is off, to close O73 (`/effort low`).
4. Then O76, O82 and O81, O83 (with item 71's O83 warnings by push), O84 (three versions), O85, O80.

```
git add .
git commit -m "Nov9.5-DefaultId - DEFAULT IDS UNLESS ASSIGNED (ISSUE-NOV8.101-O59, ISSUE-NOV8.13-O56)

- O59: a Sensor started at boot connected as PENDING_IDENTITY; both services now create their default ID (T, V) before connecting.
- O56 closed by user decision: a Sensor keeps T unless it was given another ID (already the behaviour; no code change).
- Index row Nov9.4-OwnVibration filled (a855940). SOT 1.405, chapter 167, IdentityAtConnectTest.
- TESTS: run_tests passed (0 failures), ProductionReadinessAuditTest 3/3 on the A15, release audit clean.
- VERSION: Nov9.5-DefaultId, versionCode 75."
git tag Nov9.5-DefaultId
git push origin main --tags
```

# Project Handover - Version Nov9.5-DefaultId (Default IDs Unless Assigned)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov9.5-DefaultId^{commit}`.
- **User decisions (2026-11-09)**: a Sensor keeps the default ID `T` unless it was already given another ID (no generated or required IDs). Version name from the user. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`) has the Nov9.5 debug builds (built before the version bump, versionCode 74); Monitor `V` paired to `T`. The Sensor service needs one tap on `System Idle` after the last install. S21FE in personal use; never test on it.
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4. Rebuild the release APKs after a version bump before `install_release.ps1`, or Android refuses a downgrade.

## 🟢 Status: PASSED (Nov9.5-DefaultId)
- ISSUE-NOV8.101-O59 fixed, ISSUE-NOV8.13-O56 closed by decision. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1 test assembleDebug assembleRelease`: 0 failures; warnings only the known O57 Gradle lines. `ProductionReadinessAuditTest` 3/3 on the A15.
- Release audit: Sensor 299 ms, Monitor 595 ms, 0 Davey, StackLog, fatal.
- O59 was not reproduced on the A15: its Sensor already has `T`, and clearing it would unpair the Monitor; covered by `IdentityAtConnectTest`.

**Audit line**: Nov9.5-DefaultId: [SOT Count: 979 (Rules: 479), Open: H:0, M:2, L:14, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 322), QA: 101]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 55, ISSUE-NOV9.3-O64: a small test fix (`/effort low`).
3. Item 56, ISSUE-NOV9.4-O66: pause the noise alarm while the phone rings, reusing `OwnVibrationWatch` (`/effort medium`).
4. Then the Work Queue in `DOCS/issues.md`.

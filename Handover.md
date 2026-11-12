```
git add .
git commit -m "Nov12.6 - TWO-PHONE TEST SESSION, DOCS ONLY (ISSUE-NOV12.6-O92, O93, O94, O95)

- Nov12.5 deployed on the A15 and the S21FE, monitored, all tests without a person run; no repairs.
- O92 (Medium): an update or am instrument leaves SensorService stopped until the System Idle tap.
- O93 (Medium): the Monitor's instrumented tests unpair the real Monitor (both phones).
- O94 (Low): run_tests.ps1 undercounts when Android test tasks are up to date.
- O95 (Low): BackfillGapRecoveryTest fresh-pairing test flaky on the S21FE.
- Index row Nov12.5 filled (8293c85).
- TESTS: unit 632 passed; instrumented A15 53+42, S21FE 52/53 (O95); release audits clean.
- VERSION: Nov12.6, versionCode 98."
git tag Nov12.6
git push origin main --tags
```

# Project Handover - Version Nov12.6 (Two-Phone Test Session)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.6^{commit}`.
- **Docs-only version**: no app code changed; the phones run Nov12.5 builds (versionCode 97), which is correct.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after (2026-11-12: allowed for this session); when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov12.5 debug Sensor `T` (service running) and Monitor `V` paired to `T`, airplane mode off. S21FE (`R5CRC14PG4F`): release Monitor Nov12.5, paired `V` to `T` (it was already paired when connected), Global Mute on. Test packages removed from both.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; full unit count needs `run_tests.ps1 cleanTest cleanTestDebugUnitTest cleanTestReleaseUnitTest test` (O94); after `am instrument` re-pair the Monitor (O93) and tap the Sensor's `System Idle` (O92).

## 🟢 Status: PASSED (Nov12.6)
- No issue closed; new gaps O92, O93 (Medium), O94, O95 (Low). Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 632 passed; instrumented A15 Monitor 53/53, Sensor 42/42; S21FE Monitor 52/53 (O95 flaky, rerun 9/9).
- Release cold-start audits clean on both phones; link mean lag about 0.35 s.
- Not run: chapters needing a person; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov12.6: [SOT Count: 998 (Rules: 498), Open: H:0, M:8, L:16, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 357), QA: 120]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. O92: restart the Sensor after an app update (`/effort high`).
3. O93: the Monitor's instrumented tests must not unpair the real Monitor (`/effort high`).
4. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
5. Then O83, O84 (three versions), O85, O80; O94, O95 when convenient.

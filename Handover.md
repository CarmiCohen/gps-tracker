```
git add .
git commit -m "Nov15.0 - TESTS USE THEIR OWN IDENTITY STORE (ISSUE-NOV12.6-O93)

- O93: ForensicIdentityManager takes its store file; new IdentityModule; the instrumented tests of both apps use a test file, so am instrument keeps the real pairing and identity.
- Sensor ForensicPersistenceAuditTest had the same defect; MapJitterAuditTest restore removed.
- Index row Nov12.7 filled (632555c).
- TESTS: unit 640; A15 Monitor 54/54, Sensor 43/43, pairing kept; ProductionReadinessAuditTest 3/3; release audits clean.
- VERSION: Nov15.0, versionCode 100."
git tag Nov15.0
git push origin main --tags
```

# Project Handover - Version Nov15.0 (Tests Use Their Own Identity Store)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov15.0^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov15.0 debug Sensor `T` (service running) and Monitor `V` paired to `T` (reinstalled this session: it was missing; home points empty); test APKs installed. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, paired `V` to `T`, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; full unit count needs `run_tests.ps1 cleanTest cleanTestDebugUnitTest cleanTestReleaseUnitTest test` (O94); `am instrument` no longer unpairs, but keep the phone unlocked (a lock gives Compose "No compose hierarchies found") and airplane mode on (O90).

## 🟢 Status: PASSED (Nov15.0)
- ISSUE-NOV12.6-O93 closed (the 2 h test process was Android's cached empty process, not a leak). Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 640 passed; A15 `am instrument` Monitor 54/54, Sensor 43/43, Monitor `V` to `T` and Sensor `T` kept.
- `ProductionReadinessAuditTest` 3/3; release cold-start audits clean (Sensor and Monitor).
- Not run: chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov15.0: [SOT Count: 1000 (Rules: 500), Open: H:0, M:6, L:17, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 362), QA: 122]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O83: Sensor behaviour settings move to the Monitor (`/effort high`).
4. Then O84 (three versions), O85, O80; O94, O95, O96 when convenient.

```
git add .
git commit -m "Nov12.7 - AN UPDATE DOES NOT STOP THE SENSOR (ISSUE-NOV12.6-O92)

- O92: BootReceiver also starts SensorService on MY_PACKAGE_REPLACED; opening the app starts it without the System Idle tap; one shared permission check.
- New gap O96 (Low): a console STOP is not remembered across reboot, update or launch.
- Index row Nov12.6 filled (26805dd).
- TESTS: unit 640 passed (BootReceiverTest 4/4); A15 update and force-stop restarts verified; release audits clean.
- VERSION: Nov12.7, versionCode 99."
git tag Nov12.7
git push origin main --tags
```

# Project Handover - Version Nov12.7 (An Update Does Not Stop The Sensor)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov12.7^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov12.7 debug Sensor `T` (service running) and Monitor `V` paired to `T`. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, paired `V` to `T`, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; full unit count needs `run_tests.ps1 cleanTest cleanTestDebugUnitTest cleanTestReleaseUnitTest test` (O94); after `am instrument` re-pair the Monitor (O93) and open the Sensor app (no tap needed now).

## 🟢 Status: PASSED (Nov12.7)
- ISSUE-NOV12.6-O92 closed; new gap ISSUE-NOV12.7-O96 (Low). Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 640 passed; A15: update over the running Sensor restarted it alone (`0x209`), force-stop then app open gave `0x289` without a tap.
- Release cold-start audits clean on the A15 (Sensor and Monitor).
- Not run: `ProductionReadinessAuditTest` (an instrumented Monitor run unpairs the Monitor, O93); chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov12.7: [SOT Count: 999 (Rules: 499), Open: H:0, M:7, L:17, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 360), QA: 121]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. O93: the Monitor's instrumented tests must not unpair the real Monitor (`/effort high`).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. Then O83, O84 (three versions), O85, O80; O94, O95, O96 when convenient.

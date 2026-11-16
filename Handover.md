```
git add .
git commit -m "Nov16.2 - AUTO BACKUP (ISSUE-NOV10.8-O80 part 2)

- O80: setup stores moved out of no_backup to files/datastore (one-time move, BackupStoreFile); include-only Auto Backup and device-transfer rules in both apps.
- Backed up: Sensor settings + identity; Monitor settings, identity, trail database. Not: event logs, graph history, forensic buffer.
- O80 closed (part 1 Nov16.1); O16 note: a restore brings the last backup's sequence floor.
- Index row Nov16.1 filled (66d7e41).
- TESTS: unit 828 PASSED; A15 move on update, bmgr backupnow/pm clear/restore; instrumented 5/5; release audits clean.
- VERSION: Nov16.2, versionCode 108."
git tag Nov16.2
git push origin main --tags
```

# Project Handover - Version Nov16.2 (Auto Backup)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov16.2^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.2 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.2 debug Sensor `T` and Monitor `V`, paired, both test APKs Nov16.2; data was cleared by `pm clear` and restored by `bmgr restore`, permissions granted again by adb; backup transport back on GMS; airplane mode off; the Monitor showed an alarm from the Sensor after the restore, left for the user to acknowledge. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; chapter 195.3 for the backup check (`bmgr restore` needs the token: `bmgr list sets`); `TESTS/device_audit.cmd <serial> <package>` audits a running app, so start it first.

## 🟢 Status: PASSED (Nov16.2)
- ISSUE-NOV10.8-O80 part 2 (Auto Backup) done, O80 closed; ISSUE-NOV5.2-O16 note added; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: PASSED (828); `assembleDebug`, `assembleRelease` and androidTest APKs built, no new warning.
- A15: chapters 195.2 and 195.3 live; `ProductionReadinessAuditTest` + both `IdentityIsolationTest` 5/5; release start audits clean.
- Not run: a real Google cloud backup (local transport only), restore by uninstall and reinstall, trail row count before and after the restore (the WAL was empty after it); full instrumented suites; chapters H or S; 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.2: [SOT Count: 1008 (Rules: 508), Open: H:0, M:3, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 393), QA: 130]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O98, O99, O100, O86, O94 to O97 when convenient.

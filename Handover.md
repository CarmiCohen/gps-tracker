```
git add .
git commit -m "Nov15.6 - THE GRAPHS RETURN (ISSUE-NOV10.8-O84 part 3)

- O84 part 3: a Graphs button in both apps; one sample a minute kept 7 days (peaks for vibration, tilt, noise; GNSS, link, battery).
- The Monitor asks the Sensor for the samples it lacks while its Graphs are open (graph_request / graph_samples; relay unchanged) and adds its own connection.
- O84 done. New gap O100. Index row Nov15.5 filled (ab41e44).
- TESTS: unit 797; instrumented Sensor 40/40, Monitor 56/56; ProductionReadinessAuditTest 3/3; release audits clean; A15 live check.
- VERSION: Nov15.6, versionCode 105."
git tag Nov15.6
git push origin main --tags
```

# Project Handover - Version Nov15.6 (The Graphs Return)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov15.6^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov15.6 needed no relay change (`graph_request` and `graph_samples` ride `settings_update`).
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue". Graph data (2026-11-16): the Sensor's own samples, not rows.
- **Phones**: A15 (`R58X40GV2AR`): Nov15.6 debug Sensor `T` and Monitor `V`, paired, data kept; both test APKs installed; airplane mode off; Sensor service running. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (`am instrument` with installed test APKs keeps the apps' data; a Monitor run can outlast a 2-minute tool wait: run it in the background into a file); chapter 192.5 for the live Graphs check (the menu bar is mirrored on the A15: find buttons by `content-desc` in a `uiautomator dump`).

## 🟢 Status: PASSED (Nov15.6)
- ISSUE-NOV10.8-O84 part 3 (Graphs) done, so O84 is closed; new Low gap ISSUE-NOV15.6-O100; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 797 passed; `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 192.5 live (two axis display faults fixed there); instrumented Sensor 40/40, Monitor 56/56; `ProductionReadinessAuditTest` 3/3; release cold-start audits clean, and the release Monitor with its Graphs open.
- Not run: stress protocol (no change in that area); chapters marked H or S; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov15.6: [SOT Count: 1005 (Rules: 505), Open: H:0, M:4, L:21, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 385), QA: 127]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O85: a home point list with a delete button per point in the Monitor's `HomePointEditor` (`/effort medium`).
4. Then O80 (backup and Export/Import of settings, `/effort high`); O98, O99, O100, O86, O94 to O97 when convenient.

```
git add .
git commit -m "Nov15.5 - THE DETAILED INFO RETURNS (ISSUE-NOV10.8-O84 part 2)

- O84 part 2: the menu's Info opens a live Info screen in both apps (Service, Connection with latency, Position with satellite list, Sensors, Phone); Setup keeps the dialog, renamed.
- The Monitor shows the Sensor's Info (sensor_info, sent only while asked by info_request) and its own phone; relay unchanged (latency from the ping_cmd echo).
- Rows carry satellites used/in view, mean signal and noise level. New gap O99. Index row Nov15.4 filled (8a01384).
- TESTS: unit 770; instrumented Sensor 43/43, Monitor 55/55; ProductionReadinessAuditTest 3/3; release audits clean; A15 live check.
- VERSION: Nov15.5, versionCode 104."
git tag Nov15.5
git push origin main --tags
```

# Project Handover - Version Nov15.5 (The Detailed Info Returns)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov15.5^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov15.5 needed no relay change (`sensor_info` and `info_request` ride `settings_update`; latency uses `ping_cmd`).
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov15.5 debug Sensor `T` and Monitor `V`, paired, data kept (installs over the apps and `am instrument` only); both test APKs installed; airplane mode off; Sensor service running; three cleared alarms acknowledged on the Monitor during the check. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (`am instrument` with installed test APKs keeps the apps' data); chapter 191.5 for the live Info check (read screens with `uiautomator dump`); Monitor instrumented runs in airplane mode (O90).

## 🟢 Status: PASSED (Nov15.5)
- ISSUE-NOV10.8-O84 part 2 (detailed Info) done; part 3 (Graphs) open; new Low gap ISSUE-NOV15.5-O99; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 770 passed; `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 191.5 live (one fix found there: rows in the half-second batch showed as held offline); instrumented Sensor 43/43, Monitor 55/55; `ProductionReadinessAuditTest` 3/3; release cold-start audits clean, and the release Monitor with Info open.
- Not run: stress protocol (no change in that area); chapters marked H or S; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov15.5: [SOT Count: 1004 (Rules: 504), Open: H:0, M:5, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 380), QA: 126]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O84 part 3: the Graphs, with a 7-day sample store; mind O99 (backfilled rows lack the new fields) (`/effort high`).
4. Then O85, O80; O98, O99, O86, O94 to O97 when convenient.

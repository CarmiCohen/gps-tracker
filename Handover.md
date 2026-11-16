```
git add .
git commit -m "Nov16.0 - THE HOME POINT LIST (ISSUE-NOV10.8-O85)

- O85: the Monitor's HOME editor lists every point (number, coordinates) with a red X each; X and marker tap ask the same Remove Home Point N? dialog.
- Row text forced left to right (the A15's Hebrew layout reordered it).
- Index row Nov15.6 filled (9f5b386).
- TESTS: unit PASSED; instrumented Monitor 59/59 (HomePointListTest 3/3); ProductionReadinessAuditTest 3/3; release audits clean; A15 live check (Sensor got each change).
- VERSION: Nov16.0, versionCode 106."
git tag Nov16.0
git push origin main --tags
```

# Project Handover - Version Nov16.0 (The Home Point List)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov16.0^{commit}`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov16.0 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.0 debug Sensor `T` and Monitor `V`, paired, data kept, 0 home points (as before the session); both test APKs installed; airplane mode off; Sensor service running. S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes (`am instrument` with installed test APKs keeps the apps' data; Monitor runs in airplane mode, O90); chapter 193.2 for the live Home Point list check (find the X buttons by `content-desc` in a `uiautomator dump`; the layout is mirrored on the A15).

## 🟢 Status: PASSED (Nov16.0)
- ISSUE-NOV10.8-O85 (Home Point list with a remove button per point) done; no new gap; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: PASSED (427 run; the unchanged modules' test tasks up to date); `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 193.2 live (one bidi display fault fixed there); instrumented Monitor 59/59; `ProductionReadinessAuditTest` 3/3; release cold-start audits clean.
- Not run: Sensor instrumented suite (no Sensor change); stress protocol; chapters marked H or S; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.0: [SOT Count: 1006 (Rules: 506), Open: H:0, M:4, L:20, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 387), QA: 128]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O80: Auto Backup rules plus Export/Import of settings in both apps (`/effort high`).
4. O98, O99, O100, O86, O94 to O97 when convenient.

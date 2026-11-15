```
git add .
git commit -m "Nov15.3 - THE MONITOR WARNS ABOUT THE SENSOR PHONE (ISSUE-NOV10.8-O83 part 2, O97)

- O83 part 2: the Sensor sends a sensor_setup settings message (missing Phone Setup items, service faults, applied detection rev) on Monitor connect and on change (setup re-read every 30 s); relay unchanged. O83 closed.
- Monitor: amber 'SENSOR PHONE: N ITEMS NEED ATTENTION' line with a details list; Setup & Info shows setup and detection-settings status (O97 item 3).
- trackerDeliveries moved to core-network peerDeliveries (GeofenceSync.kt deleted). New idea SIMP-NOV15.3-S1. Index row Nov15.2 filled (d43bf6b).
- TESTS: unit 701; A15 live report 0.25-0.33 s, battery exemption off/on seen on the Monitor; instrumented Sensor 43/43, Monitor 54/54; ProductionReadinessAuditTest 3/3; release audits clean; stress 4 rotation clean.
- VERSION: Nov15.3, versionCode 102."
git tag Nov15.3
git push origin main --tags
```

# Project Handover - Version Nov15.3 (The Monitor Warns About The Sensor Phone)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov15.3^{commit}`. `GeofenceSync.kt` is already staged as deleted (`git rm`).
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov15.3 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or a Hilt `Could not get element`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process`; check no other session builds the project at the same time (seen in Nov15.3).
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov15.3 debug Sensor `T` and Monitor `V` paired; all four sensor switches on, sliders 0.5; battery exemption kept; Sensor and Monitor test APKs installed; airplane mode off. The A15 reports `KEEP_WHEN_UNUSED` missing (real). S21FE (`R5CRC14PG4F`): not used; release Monitor Nov12.5, Global Mute on.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; chapter 189.4 for the live warning check; on a one-phone test, acknowledge alarm screens with `KEYCODE_TAB` + `KEYCODE_ENTER` (a tap can raise a lift alarm).

## 🟢 Status: PASSED (Nov15.3)
- ISSUE-NOV10.8-O83 closed (part 2) and ISSUE-NOV15.2-O97 item 3 done; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Unit 701 passed; `assembleDebug` and `assembleRelease` built, no new warning.
- A15: chapter 189.4 live; instrumented Sensor 43/43, Monitor 54/54 (pairing kept); `ProductionReadinessAuditTest` 3/3; release cold-start audits clean; stress section 4 rotation clean.
- Not run: stress sections 1 to 3 (no controls, D) and 5 (shielding, H); chapters marked H or S in `TEST_PROCEDURE.md`; chapter 182.2 waits for the next Manual Deploy.

**Audit line**: Nov15.3: [SOT Count: 1002 (Rules: 502), Open: H:0, M:5, L:18, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 370), QA: 124]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O84: Events log, detailed Info and Graphs, likely three versions (`/effort high`).
4. Then O85, O80; O86 (O83 warnings by push), O94 to O97 when convenient.

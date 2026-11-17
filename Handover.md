```
git add .
git commit -m "Nov16.8 - WHY A GAP, AND THE GRAPHS OF AN EVENT (ISSUE-NOV15.6-O100 parts 3 and 4)

- O100 part 3: the Graphs shade and list the Sensor's service stops, from its own stop and start events.
- O100 part 4: an event's details open the Graphs centred on its time (marker, Now chip); the Events list keeps its state.
- Tests: 852 unit tests PASSED (GraphsTest +3); A15 live check and release audit clean.
- Version Nov16.8 (114)."
git tag Nov16.8
git push origin main --tags
```

# Project Handover - Version Nov16.8 (Why A Gap, And The Graphs Of An Event)

## ▶️ Resumption State (Read First)
- **Git**: Nov16.8 waits for the Git block above; the index row Nov16.8 says `pending` (the next session fills it from the tag). Nov16.7 is `693df87`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `device_audit.cmd <serial> <package>` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.8 debug Sensor and Monitor, paired, Sensor service running, ringer on vibrate. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112), paired to `T`, Global Mute on; untouched.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov16.8)
- ISSUE-NOV15.6-O100 parts 3 and 4 done (part 1 and the new part 7 stay open); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 852 unit tests and both builds PASSED, no new warning; A15: chapter 199.3 verified in both apps, `ProductionReadinessAuditTest` 3/3, release start audits clean; debug builds restored.

**Audit line**: Nov16.8: [SOT Count: 1013 (Rules: 513), Open: H:0, M:3, L:19, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 404), QA: 134]

## 🎯 Next Resumption Focus
1. Fill the index row Nov16.8 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O100 parts 1 and 7, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4 when convenient.

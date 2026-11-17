```
git add -A
git commit -m "Nov17.1 - ONE MAP PATH (SIMP-NOV17.0-S1, SIMP-NOV5.0-S1)

- SIMP-NOV17.0-S1 and SIMP-NOV5.0-S1: the unused pre-split map stack (MapComponents.kt, MapController.kt, MapOverlayManager.kt, MapModels.kt) is removed; MapFollowMode moved into MapCenterResolver.kt.
- MAP_FUNCTIONALITY_SPEC.md describes only the live map; SOT 1.338 retired, 1.441 added; chapter 201.
- User decision: Two-phone test rule (Sensor on the A15, Monitor on the S21FE) in issues.md Operational Notes and CLAUDE.md.
- Tests: 852 unit tests PASSED; both builds PASSED; A15: ProductionReadinessAuditTest 3/3, map screenshot, release audit clean.
- Version Nov17.1 (116)."
git tag Nov17.1
git push origin main --tags
```

# Project Handover - Version Nov17.1 (One Map Path)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.1 waits for the Git block above (`git add -A` records the four deleted files); the index row Nov17.1 says `pending` (the next session fills it from the tag). Nov17.0 is `d005704`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `device_audit.cmd <serial> <package>` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; with both phones connected use the Two-phone test (`DOCS/issues.md` Operational Notes, 2026-11-17); when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov17.1 debug Sensor and Monitor, paired, Sensor service running. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112), paired to `T`, Global Mute on; connected this session but untouched.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes). `run_tests.ps1` undercounts when tasks are up to date (O94): add `--rerun-tasks` for the full 852.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.1)
- SIMP-NOV17.0-S1 and SIMP-NOV5.0-S1 done (one map path); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 852 unit tests and both builds PASSED, the Monitor's `w:` lines gone; A15: chapter 201 verified (screenshot), `ProductionReadinessAuditTest` 3/3, release start audits clean; debug builds restored.

**Audit line**: Nov17.1: [SOT Count: 1015 (Rules: 515), Open: H:0, M:3, L:19, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 408), QA: 136]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.1 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O100 parts 1 and 7, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4.

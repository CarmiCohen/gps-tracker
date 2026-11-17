```
git add -A
git commit -m "Nov17.4 - SCALE AND VERSION CHECKED ON THE S21FE, TWO-PHONE TEST WITHOUT ASKING (ISSUE-NOV17.0-O101 follow-up)

- The S21FE showed no scale or version because it still ran release Nov16.6; Nov17.0 to Nov17.3 were checked only on the A15.
- Chapter 200.2 names the Monitor phone (S21FE in a Two-phone test) and checks the installed version first.
- User decision: with the S21FE connected, the Two-phone test runs without asking (CLAUDE.md, issues.md Operational Notes).
- Tests: Two-phone test, 200.2 PASSED on the S21FE; :core-shared:test PASSED (documents-only version).
- Version Nov17.4 (119)."
git tag Nov17.4
git push origin main --tags
```

# Project Handover - Version Nov17.4 (Scale And Version Checked On The S21FE, Two-Phone Test Without Asking)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.4 waits for the Git block above; the index row Nov17.4 says `pending` (the next session fills it from the tag). Nov17.3 is `853ee20`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.4 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `install_release.ps1` (installs the existing release APK: run `assembleRelease` first), `device_audit.cmd <serial> <package>`, `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking (Nov17.4), checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); when the effort level should change, stop and ask; `//` means "continue". 2026-11-17: link statistics stay in memory, Info says since when they count.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.4, service running; Monitor UNINSTALLED (Two-phone test; reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): release Monitor Nov17.4 (119), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected; with two phones use `adb -s`. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1` (no `MainActivity`). Add `--rerun-tasks` for the full 867 tests (O94).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.4)
- ISSUE-NOV17.0-O101 follow-up (documents only): chapter 200.2 PASSED on the S21FE (`Version: Nov17.4` bottom left, scale 0.2 km to 0.9 km on zoom-out); Two-phone test rule changed to run without asking; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Documents-only version (D5): `:core-shared:test` only; no full suite, builds or release audit.

**Audit line**: Nov17.4: [SOT Count: 1017 (Rules: 517), Open: H:0, M:3, L:18, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 417), QA: 139]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.4 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O98 parts 2 and 4, O102, O86, O94 to O97.

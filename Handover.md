```
git add -A
git commit -m "Nov17.6 - ICONS THAT TELL THE APPS AND THE DEVICES APART (ISSUE-NOV17.6-O103)

- O103: adaptive launcher icons: Monitor a white eye on cyan, Sensor a white pin on green; the Monitor's map shows the Monitor as a cyan dot and the Sensor as a green dot.
- Sensor trail stays blue (user decision, SIMP-NOV17.6-S1).
- Tests: 876 unit tests PASSED (--rerun-tasks), both builds; icons and dots checked by the user on the A15; release audit clean on the A15.
- Version Nov17.6 (121)."
git tag Nov17.6
git push origin main --tags
```

# Project Handover - Version Nov17.6 (Icons That Tell The Apps And The Devices Apart)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.6 waits for the Git block above; the index row Nov17.6 says `pending` (the next session fills it from the tag). Nov17.5 is `6760eeb`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.6 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `install_release.ps1` (installs the existing release APK: run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; 2026-11-17: the Sensor trail stays blue (SIMP-NOV17.6-S1).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.5 code with the Nov17.6 icons, service running; Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): disconnected by the user; release Monitor Nov17.5 (120) without the new icons, paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1`. A force-stopped Monitor does not stay offline (it is started again): use airplane mode. Add `--rerun-tasks` for the full 876 tests (O94). `audit_line.ps1` accepts only the status codes `L A T H S D N`.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.6)
- ISSUE-NOV17.6-O103 (user request) opened and closed: distinct adaptive launcher icons in the device colors; device dots on the Monitor's map in cyan (Monitor) and green (Sensor); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 876 unit tests and both builds PASSED; the user checked icons and dots on the A15 (chapter 206); release audit on the A15 clean.
- Not run: no Two-phone test (no logic change; the S21FE was disconnected).

**Audit line**: Nov17.6: [SOT Count: 1019 (Rules: 519), Open: H:0, M:3, L:17, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 423), QA: 141]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.6 from the tag (`/effort low`).
2. Install the release Monitor Nov17.6 on the S21FE when it is connected (it gets the new icon) (`/effort low`).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O102 (backfilled rows without battery and temperature), then O94 to O97, O86.

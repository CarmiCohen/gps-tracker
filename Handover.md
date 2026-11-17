```
git add -A
git commit -m "Nov17.7 - BACKFILL KEEPS BATTERY AND TEMPERATURE (ISSUE-NOV17.3-O102)

- O102: forensic buffer version 5 stores battery level, charging and temperature; backfilled rows carry them like live rows; versions 2 to 4 upgraded in place.
- SIMP-NOV17.7-S1 logged: unknown temperature and battery as null.
- Tests: 878 unit tests PASSED (--rerun-tasks), both builds; upgrade checked on the A15; release audit clean on the A15.
- Version Nov17.7 (122)."
git tag Nov17.7
git push origin main --tags
```

# Project Handover - Version Nov17.7 (Backfill Keeps Battery And Temperature)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.7 waits for the Git block above; the index row Nov17.7 says `pending` (the next session fills it from the tag). Nov17.6 is `e45e5b4`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.7 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `install_release.ps1` (installs the existing release APK: run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); 2026-11-17: O102 fixed by storing the values in the buffer (option A).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.7, service running, buffer file version 5; Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): disconnected by the user; release Monitor Nov17.5 (120), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1`. A force-stopped Monitor does not stay offline: use airplane mode. Add `--rerun-tasks` for the full 878 tests (O94). The first launch right after a release install can show one long frame (ART not compiled yet); audit the next cold start.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.7)
- ISSUE-NOV17.3-O102 closed: the forensic buffer (version 5) keeps battery, charging and temperature, so backfilled rows carry them; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 878 unit tests and both builds PASSED; buffer upgrade from version 4 checked on the A15 (207.3); release Sensor audit clean on the A15.
- Not run: 207.4 (Two-phone backfill check) and the Monitor audit: the S21FE was disconnected and the A15 has no Monitor.

**Audit line**: Nov17.7: [SOT Count: 1020 (Rules: 520), Open: H:0, M:3, L:16, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 427), QA: 142]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.7 from the tag (`/effort low`).
2. When the S21FE is connected: install the release Monitor Nov17.7 (new icon), and run 207.4 as a Two-phone test (`/effort low`).
3. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
4. O94 to O97, then O86.

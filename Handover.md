```
git add -A
git commit -m "Nov17.5 - EVERY SENSOR EVENT, AND THE SENSOR'S PRESENCE, IN THE MONITOR'S LOG (ISSUE-NOV15.4-O98 parts 2 and 4)

- O98 part 2: the Monitor logs Sensor online / Sensor offline whatever its alarm settings (AlarmBook.isTrackerOnline).
- O98 part 4: batches say since when they are complete; a Monitor that finds a hole asks (events_request) and gets every missed event, 50 a message, without holes.
- Tests: 876 unit tests PASSED (--rerun-tasks), both builds; Two-phone test PASSED (54 events missed, 56 received); release audit clean on the A15.
- Version Nov17.5 (120)."
git tag Nov17.5
git push origin main --tags
```

# Project Handover - Version Nov17.5 (Every Sensor Event, And The Sensor's Presence, In The Monitor's Log)

## ▶️ Resumption State (Read First)
- **Git**: Nov17.5 waits for the Git block above; the index row Nov17.5 says `pending` (the next session fills it from the tag). Nov17.4 is `929d32b`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov17.5 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1`, `install_release.ps1` (installs the existing release APK: run `assembleRelease` first), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first (`DOCS/issues.md` Operational Notes); 2026-11-17: Claude may switch the S21FE to airplane mode for tests, restoring everything after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov17.5, service running; Monitor UNINSTALLED (reinstall only for an A15-only test). S21FE (`R5CRC14PG4F`): release Monitor Nov17.5 (120), paired to `T`, Global Mute on; airplane mode off, Wi-Fi and Bluetooth on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected. Start the Monitor with `monkey -p com.jd.app.monitor -c android.intent.category.LAUNCHER 1`. A force-stopped Monitor does not stay offline (it is started again): use airplane mode. Add `--rerun-tasks` for the full 876 tests (O94).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov17.5)
- ISSUE-NOV15.4-O98 closed with parts 2 and 4: Sensor online/offline logged also when muted; a Monitor back from offline asks for and receives every missed Sensor event; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 876 unit tests and both builds PASSED; Two-phone test (chapter 205.4) PASSED; release audit on the A15 clean.
- Known limit: a Monitor keeping no event of its Sensor (new pairing, or Clear and restart) receives the Sensor's whole 7 days once.

**Audit line**: Nov17.5: [SOT Count: 1018 (Rules: 518), Open: H:0, M:3, L:17, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 421), QA: 140]

## 🎯 Next Resumption Focus
1. Fill the index row Nov17.5 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O102 (backfilled rows without battery and temperature), then O94 to O97, O86.

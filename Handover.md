```
git add -A
git commit -m "Nov18.11 - A SHORT RELAY DROP IS NOT AN ALARM (ISSUE-NOV18.9-O121)

- O121: the Monitor alarms on a relay drop only after 30 s lost.
- O121: the Sensor logs Detection settings applied only on a new revision.
- 225.3 on the S21FE (release Monitor Nov18.10): HUD ID, UNPAIR, HOME rows whole.
- Home points checked on the A15: both stored, 150 m, shown as the Info Home line.
- Index row Nov18.10 (1836d9b); idea SIMP-NOV18.11-S1.
- Tests: 940 unit; ProductionReadinessAuditTest (3) on the A15; audits clean.
- Version Nov18.11 (142)."
git tag Nov18.11
git push origin main --tags
```

# Project Handover - Version Nov18.11 (A Short Relay Drop Is Not An Alarm)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.11 waits for the Git block above; the index row Nov18.11 says `pending` (the next session fills it from the tag). Nov18.10 is `1836d9b`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or with unresolved references in files not changed, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` does not build: run `assembleRelease` first (SIMP-NOV18.11-S1). `run_tests.ps1`, `install_release.ps1`, `device_audit.cmd`, `audit_line.ps1 -Version <v>` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; airplane mode or Global Mute off for tests, restored after; when the effort level should change, stop and ask; `//` means "continue". O112: the user notes new false alarms as they happen.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.11, service running, no Monitor or test apks, airplane off. S21FE (`R5CRC14PG4F`): release Monitor Nov18.10 (141), paired V -> T, FENCE on, left on the Map tab. The release Monitor is debug-key signed: the debug build installs over it with `install -r -d` and keeps the pairing.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90). With two phones, read a screen with `adb -s <serial> shell uiautomator dump` (the IDE screen tools pick one phone). The Sensor's Info shows home points as one `Home` line in SENSOR - POSITION.

## 🟢 Status: PASSED (Nov18.11)
- O121 fixed (short relay drops, the settings line). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov18.11).
- 225.3 passed on the S21FE except its noise alert line (waits for a noise alert).
- Tests: `run_tests.ps1` 940 passed (known kapt warning, O72); A15: `ProductionReadinessAuditTest` 3; release cold starts clean; 226.2 passed.
- Not run: 226.3, a live relay drop on the Monitor (S21FE still runs Nov18.10; needs release Monitor Nov18.11 there).

**Audit line**: Nov18.11: [SOT Count: 1038 (Rules: 538), Open: H:0, M:3, L:12, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 467), QA: 160]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.11 from the tag (`/effort low`).
2. Release Monitor Nov18.11 on the S21FE (`assembleRelease`, then `install_release.ps1 -Serial R5CRC14PG4F -Apps monitor`), then 226.3 and the 225.3 noise line when a noise alert comes (`/effort low`).
3. O86 (the O83 warnings by push) (`/effort high`); O112 when the user has noted false alarms (`/effort high`).
4. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image).

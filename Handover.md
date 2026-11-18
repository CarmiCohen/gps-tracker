```
git add -A
git commit -m "Nov18.10 - TEXTS SHOWN WHOLE, THE NOISE JUMP PRINTED (ISSUE-NOV18.9-O117, O118, O119, O120)

- O117: the HUD keeps the phone ID whole; long telemetry wraps.
- O118: the Monitor Setup dialog scrolls; UNPAIR no longer clipped.
- O119: a Home Point row grows to show both coordinates.
- O120: the noise alert prints the compared background and the jump.
- O112 parked until the user notes false alarms; index row Nov18.9 (8e5e526).
- Tests: 936 unit; TextFitTest (3) and 19 Monitor UI tests on the A15; audits clean.
- Version Nov18.10 (141)."
git tag Nov18.10
git push origin main --tags
```

# Project Handover - Version Nov18.10 (Texts Shown Whole, The Noise Jump Printed)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.10 waits for the Git block above; the index row Nov18.10 says `pending` (the next session fills it from the tag). Nov18.9 is `8e5e526`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1`, `device_audit.cmd`, `audit_line.ps1 -Version <v>` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; airplane mode or Global Mute off for tests, restored after; when the effort level should change, stop and ask; `//` means "continue". O112: the user does not remember past false alarms and will note new ones as they happen.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.10, service running, no Monitor or test apks, airplane off. S21FE (`R5CRC14PG4F`): release Monitor Nov18.8 (139), paired V -> T, FENCE on (not touched this session). The release Monitor is debug-key signed: the debug build installs over it with `install -r -d` and keeps the pairing.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90). In Compose tests `hasVisualOverflow` is unreliable for one-line or centered text; `TextFitTest.assertTextFits` shows the working check.

## 🟢 Status: PASSED (Nov18.10)
- O117, O118, O119, O120 fixed; O112 parked. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov18.10).
- Tests: `run_tests.ps1` 936 passed (known kapt warning, O72); A15: `TextFitTest` 3, Monitor UI tests 19, `ProductionReadinessAuditTest` 3; release cold starts clean.
- Not run: 225.3 live look on the S21FE (needs release Monitor Nov18.10 there).

**Audit line**: Nov18.10: [SOT Count: 1037 (Rules: 537), Open: H:0, M:3, L:13, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 464), QA: 159]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.10 from the tag (`/effort low`).
2. 225.3: install release Monitor Nov18.10 on the S21FE (`install_release.ps1 -Serial R5CRC14PG4F -Apps monitor`) and look at the HUD ID, UNPAIR and the HOME list (`/effort low`).
3. O121: relay disconnect alarms and repeated `Detection settings applied` (`/effort medium`).
4. O86 (the O83 warnings by push) (`/effort high`); O112 when the user has noted false alarms (`/effort high`).
5. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image).

```
git add -A
git commit -m "Nov18.9 - LIVE LOOKS ON A PAIRED MONITOR (ISSUE-NOV12.6-O95, O104, O105, O108, O109, O116, O112)

- S21FE + A15: 222.2, 217.2, 223.3 passed; 218.2 half (Monitor's own ID cut off: O117).
- 215.1: full Monitor am instrument on the S21FE OK (65 tests).
- O112: the S21FE's 7-day Events read; waits for the user's false-alarm list.
- New gaps O117 to O121; index row Nov18.8 filled (5cbbe9f).
- Tests: :core-shared:test (docs-only).
- Version Nov18.9 (140)."
git tag Nov18.9
git push origin main --tags
```

# Project Handover - Version Nov18.9 (Live Looks On A Paired Monitor)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.9 waits for the Git block above; the index row Nov18.9 says `pending` (the next session fills it from the tag). Nov18.8 is `5cbbe9f`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1`, `device_audit.cmd`, `audit_line.ps1 -Version <v>` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; airplane mode or Global Mute off for tests, restored after; when the effort level should change, stop and ask; `//` means "continue". Product decisions: see the Nov18.7 Handover (tag) and `DOCS/issues.md`.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.8, service running, Sensor console open. S21FE (`R5CRC14PG4F`): release Monitor Nov18.8 (139), paired V -> T, FENCE on, test apk removed, airplane off. The release Monitor is debug-key signed: the debug build installs over it with `install -r -d` and keeps the pairing (used for 215.1).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90). Screenshots of one phone with two connected: `adb -s <serial> shell screencap -p /sdcard/x.png` then `pull` (a PowerShell `>` redirect corrupts the PNG).
- **O112 data**: the S21FE's Events export is `/sdcard/Download/events-20261118-194521.txt` (copy in `build/o112/`).

## 🟢 Status: PASSED (Nov18.9)
- Docs-only version: live looks 222.2, 217.2, 223.3 and 215.1 passed; 218.2 failed on the Monitor's own ID (O117). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov18.9).
- O112: Events read; open until the user names the false alarms. New gaps O117 to O121.
- Tests: `:core-shared:test`. Not run: builds and release audits (no code change), 213.2's Active apps list tap.

**Audit line**: Nov18.9: [SOT Count: 1036 (Rules: 536), Open: H:0, M:3, L:17, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 461), QA: 158]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.9 from the tag (`/effort low`).
2. O112: ask the user which motion alarms of the archive's Nov18.9 list were false, then tune `MotionSentinel.kt` (`/effort high`).
3. The small UI gaps O117, O118, O119, O120 in one version (`/effort medium`); O121 needs a look at the relay reconnects.
4. O86 (the O83 warnings by push) (`/effort high`).
5. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image).

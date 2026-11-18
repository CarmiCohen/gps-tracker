```
git add -A
git commit -m "Nov18.8 - AN ACKNOWLEDGEMENT NEVER SOUNDS THE SIREN (ISSUE-NOV10.7-O75)

- O75: acknowledge() no longer clears the siren timeout; sirenEpisode() clears it only when an episode starts, so an acknowledgement after the 30 s no longer chirps.
- Two-phone check 224.1 on the S21FE (release Monitor Nov18.8) with the A15 Sensor: no siren start after ACKNOWLEDGE.
- Index row Nov18.7 filled (072430b); the missing Nov18.7 dashboard row added.
- Tests: run_tests PASSED, both builds.
- Version Nov18.8 (139)."
git tag Nov18.8
git push origin main --tags
```

# Project Handover - Version Nov18.8 (An Acknowledgement Never Sounds The Siren)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.8 waits for the Git block above; the index row Nov18.8 says `pending` (the next session fills it from the tag). Nov18.7 is `072430b`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.8 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd`, `audit_line.ps1 -Version <v>` are in `TESTS/`. Trust the Gradle build over the IDE's "unresolved" errors.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue". Product decisions (O9, O86, O96, O109, O111 to O116, SIMP-NOV17.6-S1): see the Nov18.7 Handover (tag) and `DOCS/issues.md`.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.8, service running, on USB, battery state reset; Monitor and the test apks not installed. S21FE (`R5CRC14PG4F`): release Monitor Nov18.8 (139), installed with the user's consent 2026-11-18, paired with the A15's Sensor `T`, left on the home screen.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90), and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'`. A power alarm can be raised with `adb -s R58X40GV2AR shell dumpsys battery unplug` (restore with `reset`; chapter 224).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.8)
- ISSUE-NOV10.7-O75 done; details: `STATUS/RESOLUTION_ARCHIVE.md`; chapter 224, SOT 1.463.
- S21FE + A15: chapter 224.1 passed (no siren start after an acknowledgement past the 30 s timeout). `run_tests.ps1`: PASSED, both builds.
- Not run: release audits (Monitor-only change, checked on the release build), 223.3, 222.2, 218.2, 217.2, 215.1 (now possible: the S21FE runs Nov18.8), 213.2's Active apps list tap.

**Audit line**: Nov18.8: [SOT Count: 1036 (Rules: 536), Open: H:0, M:3, L:12, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 461), QA: 158]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.8 from the tag (`/effort low`).
2. With the S21FE paired: the live looks 223.3, 222.2, 218.2, 217.2, 215.1 and O112's Events read (`/effort low`).
3. O86 (the O83 warnings by push) (`/effort high`).
4. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image).
5. With a person at the A15: 213.2's Active apps list tap; at the next Manual Deploy on Render: chapter 182.2.

```
git add -A
git commit -m "Nov18.6 - THE MAP SHOWS THE VERSION NAME ALONE (ISSUE-NOV18.6-O116)

- O116: the map's bottom-left label shows the version name without 'Version:' (user request 2026-11-18).
- Tests: UnpairConfirmationTest.theMapShowsTheVersion checks the name and no prefix (A15, 11 OK).
- Index row Nov18.5 filled (48097b6).
- Chapter 211.2 passed on the A15 with the user (S21FE Monitor); O86 user decision: no Render payment; O75 data.
- Tests: run_tests 934 PASSED, both builds; A15 audits clean.
- Version Nov18.6 (137)."
git tag Nov18.6
git push origin main --tags
```

# Project Handover - Version Nov18.6 (The Map Shows The Version Name Alone)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.6 waits for the Git block above; the index row Nov18.6 says `pending` (the next session fills it from the tag). Nov18.5 is `48097b6`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.6 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or report unresolved functions that exist, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1` are in `TESTS/`. `ProductionReadinessAuditTest` is in the Sensor's test apk (`com.jd.app.sensor.test`). The IDE often shows false "unresolved" errors in new files; trust the Gradle build.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored, and any return to the console restarts the service (O96, O115); notification tap only (O113); the power alarm follows plugged in, not charging (O114); a noise must be loud 2 s within 3 s (O112); user-facing texts say "points", not "rows" (Nov18.5); the map's version label is the name alone (O116); no payment for an always-awake Render relay (O86, 2026-11-18).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.6, service running, on USB; Monitor and the test apks not installed; airplane off. S21FE (`R5CRC14PG4F`): release Monitor Nov17.15 (129), paired to `T`, its siren started on each alarm in Nov18.6 (mute state not checked); only its logcat was read.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90), and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'`. A debug Monitor on the A15 pairs with `T` from the wizard (`Pair Device` at `540 1024`, after the location and notification prompts); map buttons and Sensor console taps: see the Nov18.5 Handover (tag) and `DOCS/issues.md` Operational Notes.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.6)
- ISSUE-NOV18.6-O116 done: the map label shows `Nov18.6`, not `Version: Nov18.6`; the pairing wizard keeps `Version:`; details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `run_tests.ps1`: 934 tests PASSED, both builds, no new warning; A15: `UnpairConfirmationTest` 11 OK, `ProductionReadinessAuditTest` OK, release Sensor and Monitor audits `davey_over_500ms=0 stacklog=0 fatal=0`.
- 211.2 passed with the user (A15 Sensor, S21FE Monitor): every alert reached the S21FE within 1 s and started the siren; the user noticed some late because the Monitor was behind WhatsApp (open question: siren or only a pop-up). O75's chirp seen at each acknowledgement (issues.md).
- Not run: 222.2 (live look; needs a paired Monitor), 218.2, 217.2, 215.1 (S21FE Monitor), 213.2's Active apps list tap.

**Audit line**: Nov18.6: [SOT Count: 1035 (Rules: 535), Open: H:0, M:3, L:15, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 457), QA: 157]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.6 from the tag (`/effort low`).
2. O108 (show or hide the fence) and O109 (the menu's setup icons): the user said go ahead (`/effort medium`); then O75 (the acknowledgement chirp, now reproducible).
3. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image), consent to install the current release Monitor on the S21FE.
4. When the S21FE may be used: install the current release Monitor (with consent), then chapters 222.2, 218.2, 217.2, 215.1 and O112's Events read (`/effort low`).
5. With a person at the A15: 213.2's Active apps list tap; at the next Manual Deploy on Render: chapter 182.2.

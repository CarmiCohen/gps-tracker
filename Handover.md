```
git add -A
git commit -m "Nov18.7 - SHOW OR HIDE THE FENCE, ONE SETUP ICON (ISSUE-NOV17.7-O108, O109)

- O108: a FENCE map button shows or hides the geofence circles (Home markers stay); the circle is fainter (alpha 20/160).
- O109: one setup icon in both apps (user decision 2026-11-18): Monitor unpairs from Setup > UNPAIR (still asks), Sensor opens Settings from Setup > SETTINGS; the gear is gone.
- Tests: UnpairConfirmationTest + HudLayoutAuditTest 16 OK on the A15 (unpair tests go Setup > UNPAIR, none removed; new theFenceButtonToggles).
- Index row Nov18.6 filled (406fa6f).
- Tests: run_tests 934 PASSED, both builds.
- Version Nov18.7 (138)."
git tag Nov18.7
git push origin main --tags
```

# Project Handover - Version Nov18.7 (Show Or Hide The Fence, One Setup Icon)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.7 waits for the Git block above; the index row Nov18.7 says `pending` (the next session fills it from the tag). Nov18.6 is `406fa6f`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy. Nov18.7 needed no relay change.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or report unresolved functions that exist, run `gradlew --stop` and `gradlew clean`, or build with `"-Pkotlin.compiler.execution.strategy=in-process"`. `run_tests.ps1`, `install_release.ps1` (run `assembleRelease` first; `-Serial`, `-Apps`), `device_audit.cmd <serial> <package>` (start the app first), `audit_line.ps1 -Version <v>` (pass the version, the prompt does not take input from the agent) are in `TESTS/`. `ProductionReadinessAuditTest` is in the Sensor's test apk. The IDE often shows false "unresolved" errors in new files; trust the Gradle build.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; Claude may switch the S21FE to airplane mode or Global Mute off for tests, restoring after; when the effort level should change, stop and ask; `//` means "continue"; the Sensor trail stays blue (SIMP-NOV17.6-S1); a debounced event kind alone still sounds the siren (O111); a console STOP is not stored, and any return to the console restarts the service (O96, O115); notification tap only (O113); the power alarm follows plugged in, not charging (O114); a noise must be loud 2 s within 3 s (O112); user-facing texts say "points", not "rows" (Nov18.5); the map's version label is the name alone (O116); no payment for an always-awake Render relay (O86); one setup icon (wrench) in both apps (O109, 2026-11-18).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.7, service running, on USB; Monitor and the test apks not installed; airplane off. S21FE (`R5CRC14PG4F`): release Monitor Nov17.15 (129), untouched in Nov18.7 (it is Android Studio's selected device, so `take_screenshot` shows the S21FE, not the A15).
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90), and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'`. Unpair on the Monitor is now Setup > UNPAIR. Map buttons and Sensor console taps: see the Nov18.5 Handover (tag) and `DOCS/issues.md` Operational Notes.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov18.7)
- ISSUE-NOV17.7-O108 and O109 done; details: `STATUS/RESOLUTION_ARCHIVE.md`; chapter 223, SOT 1.462.
- A15: `UnpairConfirmationTest` + `HudLayoutAuditTest` 16 OK; `run_tests.ps1`: 934 tests PASSED, both builds, no new warning.
- Not run: 223.3 (live look at FENCE and the menus; needs a paired Monitor), release audits, 222.2, 218.2, 217.2, 215.1 (S21FE Monitor), 213.2's Active apps list tap.

**Audit line**: Nov18.7: [SOT Count: 1035 (Rules: 535), Open: H:0, M:3, L:13, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 460), QA: 158]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.7 from the tag (`/effort low`).
2. O75 (the acknowledgement chirp, reproducible at each acknowledgement) (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image), consent to install the current release Monitor on the S21FE.
4. When the S21FE may be used: install the current release Monitor (with consent), then chapters 223.3, 222.2, 218.2, 217.2, 215.1 and O112's Events read (`/effort low`).
5. With a person at the A15: 213.2's Active apps list tap; at the next Manual Deploy on Render: chapter 182.2.

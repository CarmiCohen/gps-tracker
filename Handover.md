```
git add -A
git commit -m "Nov19.1 - THE CLOSE X IS ALWAYS ON SCREEN (ISSUE-NOV10.7-O74, SIMP-NOV10.8-S1)

- O74 + SIMP-NOV10.8-S1: one core-ui OverlayHeader (wrapping title, actions, Close X, Back) for every overlay of both apps.
- Finding: the S21FE's 1.8 s arrival lag was mostly a 1.2 s clock difference (Operational Note).
- Index row Nov19.0 (554770b); Git routine: Claude commits, tags and pushes at Completion step 9.
- Tests: 956 unit; SetupNavigationTest 7/7, UnpairConfirmationTest 12/12 and device audit on the A15.
- Version Nov19.1 (145)."
git tag Nov19.1
git push origin main --tags
```

# Project Handover - Version Nov19.1 (The Close X Is Always On Screen)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.1 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for Nov19.2.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` does not build: run `assembleRelease` first (SIMP-NOV18.11-S1). Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 (Monitor battery) only once the project is otherwise complete. 2026-11-19: do not force deep idle on the S21FE for 227.4.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov19.1 code (labelled Nov19.0, built before the bump), service running, exemption in place, airplane off, no Monitor. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov18.12 (143), paired V -> T; the user restores Global Mute.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with airplane mode (Operational Notes). Cross-phone arrival lag includes the clock difference (Operational Notes).

## 🟢 Status: PASSED (Nov19.1)
- ISSUE-NOV10.7-O74 with SIMP-NOV10.8-S1 fixed: `OverlayHeader` in all ten overlays; Back owned by the header. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.1).
- Tests: 956 unit (one known kapt warning, O72); A15 `SetupNavigationTest` 7/7, `UnpairConfirmationTest` 12/12; release cold starts clean (0 Davey, 0 StackLog).
- 228.2 checked on the A15 Sensor (Configuration, Detection Settings, Events); the Monitor overlays on the narrow S21FE not looked at (disconnected).
- Not run: 227.4 (the S21FE left; forced idle declined).

**Audit line**: Nov19.1: [SOT Count: 1040 (Rules: 540), Open: H:0, M:2, L:11, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 473), QA: 162]

## 🎯 Next Resumption Focus
1. When the S21FE is connected: 228.2 on its Monitor (Alerts X visible, the O74 symptom) and 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small A15-only items: O90 (push crashes Monitor test runs), SIMP-NOV18.11-S1 (install script builds first).

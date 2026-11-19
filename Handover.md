```
git add -A
git commit -m "Nov19.4 - TEST RUNS IGNORE PUSHES, ONE FOLDER SAVER (ISSUE-NOV12.3-O90, SIMP-NOV19.3-S1)

- O90: HiltTestRunner turns MonitorMessagingService off for a run and back on at its end, so a push cannot crash am instrument runs; no airplane mode needed.
- SIMP-NOV19.3-S1: core-ui rememberGpsTrackerSaver holds the GPS-Tracker save rule once for the three save paths.
- Index row Nov19.3 (4c4cd90).
- Tests: 964 unit; A15 full Monitor run 76 (1 needed the notification grant, then OK), MessagingServiceOffDuringRunsTest, Sensor EXPORT to GPS-Tracker; ProductionReadinessAuditTest 3/3, release Sensor audit.
- Version Nov19.4 (148)."
git tag Nov19.4
git push origin main --tags
```

# Project Handover - Version Nov19.4 (Test Runs Ignore Pushes, One Folder Saver)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.4 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for Nov19.5.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` builds first (`-NoBuild` to skip, `-Apps sensor` for one app); on the A15 (no Monitor) install only the Sensor. `device_audit.cmd <serial> <package>` needs the app running (cold start it with `monkey -p <package> -c android.intent.category.LAUNCHER 1`); the first start right after an install may show one slow first frame, so repeat the cold start. Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 (Monitor battery) only once the project is otherwise complete. 2026-11-19: no forced deep idle on the S21FE for 227.4; Monitor orange-brown, Sensor green; Monitor icon the tractor, Sensor icon the pin; saved files in `Download/GPS-Tracker`; the menu bar shows the Sensor's motion state.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov19.4 code, running, airplane off, no Monitor. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov18.12 (143), paired V -> T; the user restores Global Mute.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). Monitor runs no longer need airplane mode (O90); a fresh Monitor install needs the `POST_NOTIFICATIONS` grant before a full run. Cross-phone arrival lag includes the clock difference.

## 🟢 Status: PASSED (Nov19.4)
- ISSUE-NOV12.3-O90 and SIMP-NOV19.3-S1 fixed. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.4).
- Tests: 964 unit (one known kapt warning, O72); A15 full Monitor `am instrument` with airplane mode off and the Sensor running, 76 tests (`SensorSetupNoticeDeviceTest` needed the fresh install's notification grant, then OK); service on again after the runs; Sensor EXPORT straight to `Download/GPS-Tracker`; `ProductionReadinessAuditTest` 3/3; release Sensor cold start clean.
- Not run: a real push during a run (the A15 Monitor is unpaired), the Monitor's EXPORT and trail SAVE (230.3), the Monitor release audit (no Monitor on the A15).

**Audit line**: Nov19.4: [SOT Count: 1043 (Rules: 543), Open: H:0, M:2, L:10, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 481), QA: 165]

## 🎯 Next Resumption Focus
1. When the S21FE is connected: install release Monitor Nov19.4 there, then 230.2/230.3 (bar, icon, GPS-Tracker saves), 229.2, 228.2 on its Monitor and 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small A15-only ideas: SIMP-NOV18.12-S1 (one push decoder), SIMP-NOV6.3-S1 (delete `HudViewState`).

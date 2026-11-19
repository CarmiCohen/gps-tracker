```
git add -A
git commit -m "Nov19.3 - TRACTOR ICON, GPS-TRACKER FOLDER, MOTION IN THE MENU BAR (ISSUE-NOV19.3-O124 TO O126, SIMP-NOV19.2-S1, SIMP-NOV18.11-S1)

- O124: the Monitor launcher icon is the pre-splitting John Deere tractor photo; the Sensor keeps its green pin.
- O125: settings exports and map trail saves go straight to Download/GPS-Tracker (MediaStore, Android 10+); pickers open there.
- O126: menu bar headers MONITOR:/SENSOR: replaced by the Sensor's STATIONARY/MOVING; HUD shows the bare ID, no motion word.
- SIMP-NOV19.2-S1: named core-ui colors instead of literal hexes. SIMP-NOV18.11-S1: install_release.ps1 builds first (-NoBuild).
- Index row Nov19.2 (93eaf56).
- Tests: 964 unit; ProductionReadinessAuditTest 3/3 and device audit on the A15.
- Version Nov19.3 (147)."
git tag Nov19.3
git push origin main --tags
```

# Project Handover - Version Nov19.3 (Tractor Icon, GPS-Tracker Folder, Motion In The Menu Bar)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.3 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for Nov19.4.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` now builds first (`-NoBuild` to skip); it installs both apps, so on the A15 (no Monitor) uninstall the Monitor afterwards. `device_audit.cmd <serial> <package>` needs the app running (cold start it with `monkey -p <package> -c android.intent.category.LAUNCHER 1`). Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 (Monitor battery) only once the project is otherwise complete. 2026-11-19: no forced deep idle on the S21FE for 227.4; Monitor orange-brown, Sensor green; Monitor icon the tractor, Sensor icon the pin; saved files in `Download/GPS-Tracker`; the menu bar shows the Sensor's motion state instead of ID headers.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov19.3 code, airplane off, no Monitor. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov18.12 (143), paired V -> T; the user restores Global Mute.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with airplane mode (Operational Notes). Cross-phone arrival lag includes the clock difference (Operational Notes).

## 🟢 Status: PASSED (Nov19.3)
- ISSUE-NOV19.3-O124, O125, O126, SIMP-NOV19.2-S1 and SIMP-NOV18.11-S1 fixed. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.3).
- Tests: 964 unit (one known kapt warning, O72); A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean (0 Davey, 0 StackLog); Sensor screen dump: bar `STATIONARY`, HUD ID `T`.
- Not run: the Monitor's bar and tractor icon and a direct save to GPS-Tracker (230.2 Monitor part, 230.3); 229.2, 228.2 Monitor overlays and 227.4 also wait for the S21FE.
- New idea SIMP-NOV19.3-S1: one core-ui folder saver for the three save paths.

**Audit line**: Nov19.3: [SOT Count: 1042 (Rules: 542), Open: H:0, M:2, L:11, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 478), QA: 164]

## 🎯 Next Resumption Focus
1. When the S21FE is connected: install release Monitor Nov19.3 there, then 230.2/230.3 (bar, icon, GPS-Tracker saves), 229.2, 228.2 on its Monitor and 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small A15-only items: O90 (push crashes Monitor test runs), SIMP-NOV19.3-S1 (one folder saver).

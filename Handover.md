```
git add -A
git commit -m "Nov19.2 - THE MONITOR IS ORANGE-BROWN (ISSUE-NOV19.2-O123, SIMP-NOV17.6-S1)

- O123: core-ui MonitorBrown #D2691E and SensorGreen #10B981 replace the Monitor cyan everywhere (HUD, map, overlays, toolbars, launcher).
- SIMP-NOV17.6-S1: the Sensor trail is drawn in the Sensor green instead of blue.
- Graphs: the second tone is light blue (gold was too close to the brown main line).
- Index row Nov19.1 (0999273).
- Tests: 964 unit (DeviceColorsTest 4); ProductionReadinessAuditTest 3/3 and device audit on the A15.
- Version Nov19.2 (146)."
git tag Nov19.2
git push origin main --tags
```

# Project Handover - Version Nov19.2 (The Monitor Is Orange-Brown)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.2 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for Nov19.3.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` does not build: run `assembleRelease` first (SIMP-NOV18.11-S1); it installs both apps, so on the A15 (no Monitor) uninstall the Monitor afterwards. `device_audit.cmd <serial> <package>`. Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 (Monitor battery) only once the project is otherwise complete. 2026-11-19: do not force deep idle on the S21FE for 227.4; the Monitor color is orange-brown and the Sensor trail green (O123).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov19.2 code, service running, exemption in place, airplane off, no Monitor. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov18.12 (143), paired V -> T; the user restores Global Mute.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with airplane mode (Operational Notes). Cross-phone arrival lag includes the clock difference (Operational Notes).

## 🟢 Status: PASSED (Nov19.2)
- ISSUE-NOV19.2-O123 and SIMP-NOV17.6-S1 fixed: Monitor orange-brown everywhere, Sensor trail green; graph second tone light blue. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.2).
- Tests: 964 unit (one known kapt warning, O72); A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean (0 Davey, 0 StackLog).
- Not run: 229.2 (the colors on a device: the A15 has no paired Monitor and the S21FE was disconnected); 228.2 Monitor overlays and 227.4 still wait for the S21FE.
- New idea SIMP-NOV19.2-S1: the remaining literal hex colors to named core-ui colors.

**Audit line**: Nov19.2: [SOT Count: 1041 (Rules: 541), Open: H:0, M:2, L:11, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 475), QA: 163]

## 🎯 Next Resumption Focus
1. When the S21FE is connected: install release Monitor Nov19.2 there, then 229.2 (the new colors), 228.2 on its Monitor and 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small A15-only items: O90 (push crashes Monitor test runs), SIMP-NOV18.11-S1 (install script builds first), SIMP-NOV19.2-S1 (named colors).

```
git add -A
git commit -m "Nov22.4 - LIGHT DETECTION (ISSUE-NOV22.4-O130)

- ISSUE-NOV22.4-O130: the Sensor alerts on a sudden light rise like on a noise (LightDetector: 4x the background
  and +20 lx, 2 s within 3 s; LightMonitor); Monitor alarm 'Tracker light detected', switch and slider, Light graph.
- Chapter 237, SOT 1.476; new ideas SIMP-NOV22.4-S1, S2; index row Nov22.3 (02d0465).
- Tests: 1031 unit; A15 ProductionReadinessAuditTest 3/3; release Sensor and Monitor cold starts clean on the A15.
- Version Nov22.4 (154)."
git tag Nov22.4
git push origin main --tags
```

# Project Handover - Version Nov22.4 (Light Detection)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.4 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`; unchanged (the light settings ride the existing `sensor_behaviour` settings message).
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` builds first (`-NoBuild` to skip, `-Apps sensor` for one app). `device_audit.cmd <serial> <package>` needs the app running. Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred by the user (2026-11-19). Light detection: rise-only, on by default (2026-11-22).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.4, running as `T`, light measuring (TCS3701; room 1-2 lx), no Monitor (installed for the audit, uninstalled after). S21FE (`R5CRC14PG4F`): not connected this session; release Monitor Nov19.5 (149), paired V -> T: it ignores the light setting until it gets a Nov22.4 Monitor.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). The Sensor logs light as `REV_Light`. After a Sensor `am instrument` run, check its ID with `device_check.ps1 -App sensor -Command STATE` (`self=T`). The Sensor console opens on a "System Idle" lock screen: one tap unlocks it.

## 🟢 Status: PASSED (Nov22.4)
- ISSUE-NOV22.4-O130 done (user request). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.4).
- Tests: 1031 unit (new `LightDetectorTest`, `LightAlarmTest`, light cases in six existing tests; no test removed); A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean (Sensor 491 ms, Monitor 583 ms, A15).
- Not run: the live light alert 237.3 (postponed by the user, away from the A15) and the Monitor's alarm from a live Sensor 237.4 (S21FE not connected).

**Audit line**: Nov22.4: [SOT Count: 1049 (Rules: 549), Open: H:0, M:2, L:10, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 496), QA: 171]

## 🎯 Next Resumption Focus
1. 237.3 at the A15: a torch at its top front edge for 3 s gives a `Light alert` event (`/effort low`).
2. S21FE checks: install release Monitor Nov22.4, then 237.4 (light alarm, switch, Light graph), 233.3, 230.3, 228.2, 227.4 after a long natural idle; watch one `REV_Push` line (`/effort low`).
3. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
4. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
5. Small ideas: SIMP-NOV22.4-S1 (light on the rows, ForensicBuffer v6, `/effort high`), SIMP-NOV22.4-S2 (one rise confirmation), SIMP-NOV22.3-S1 (one settings sender, `/effort medium`).

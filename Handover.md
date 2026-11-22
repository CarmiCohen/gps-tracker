```
git add -A
git commit -m "Nov22.0 - ONE HUD MODEL (SIMP-NOV6.3-S1)

- SIMP-NOV6.3-S1: the unused second HUD model HudViewState (core-ui ViewState.kt) is deleted; HudState is the only one.
- OneHudModelTest guards it (chapter 234, SOT 1.473); index row Nov19.6-HUD (432f17d).
- Tests: 986 unit; A15 ProductionReadinessAuditTest 3/3; release Sensor and Monitor cold starts clean on the A15.
- Version Nov22.0 (151)."
git tag Nov22.0
git push origin main --tags
```

# Project Handover - Version Nov22.0 (One HUD Model)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.0 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` builds first (`-NoBuild` to skip, `-Apps sensor` for one app). `device_audit.cmd <serial> <package>` needs the app running. Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred by the user (2026-11-19).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.0, running as `T`, no Monitor (installed for the audit, uninstalled after). S21FE (`R5CRC14PG4F`): not connected this session; release Monitor Nov19.5 (149), paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). After a Sensor `am instrument` run, check its ID with `device_check.ps1 -App sensor -Command STATE` (`self=T`). The Sensor console opens on a "System Idle" lock screen: one tap unlocks it.

## 🟢 Status: PASSED (Nov22.0)
- SIMP-NOV6.3-S1 done (user choice; the S21FE was not connected). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.0).
- Tests: 986 unit (new `OneHudModelTest`); A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean (Sensor and Monitor, A15).
- Not run: the S21FE checks (not connected).

**Audit line**: Nov22.0: [SOT Count: 1046 (Rules: 546), Open: H:0, M:2, L:10, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 488), QA: 168]

## 🎯 Next Resumption Focus
1. S21FE checks: install release Monitor Nov22.0, then 233.3 (one HUD line), 230.3 (GPS-Tracker saves), 228.2 (overlay X), 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small ideas: SIMP-NOV18.12-S1 (one push decoder, `/effort high`), SIMP-NOV6.1-S1 (shared device status), SIMP-NOV19.5-S1 (deferred by the user).

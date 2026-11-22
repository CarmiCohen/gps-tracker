```
git add -A
git commit -m "Nov19.6-HUD - ONE HUD LINE PER PHONE (ISSUE-NOV19.6-O129)

- O129: each phone's HUD is one line: ID, LEDs, data, speed; no battery or thermometer icons, no A: or AGE:;
  accuracy from 9.95 m without decimal; km/h in a tiny font; the data shrinks (9 to 6 sp) instead of wrapping.
- SIMP-NOV19.6-S1 logged (Compose autoSize after a BOM upgrade); index row Nov19.5 (8920635).
- Tests: 984 unit; A15 HudLayoutAuditTest + TextFitTest 8/8, SensorHudValuesTest 5/5, ProductionReadinessAuditTest 3/3;
  release Sensor and Monitor cold starts clean on the A15.
- Version Nov19.6-HUD (150)."
git tag Nov19.6-HUD
git push origin main --tags
```

# Project Handover - Version Nov19.6-HUD (One HUD Line Per Phone)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.6-HUD committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` builds first (`-NoBuild` to skip, `-Apps sensor` for one app). `device_audit.cmd <serial> <package>` needs the app running. Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. 2026-11-19: SIMP-NOV19.5-S1 deferred; the HUD is one line per phone (O129).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov19.6-HUD, running as `T` (an instrumented run had reset the ID to `tracker`; restored), no Monitor. S21FE (`R5CRC14PG4F`): not connected this session; release Monitor Nov19.5 (149), paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). After a Sensor `am instrument` run, check its ID with `device_check.ps1 -App sensor -Command STATE` (`self=T`). The Sensor console opens on a "System Idle" lock screen: one tap unlocks it.

## 🟢 Status: PASSED (Nov19.6-HUD)
- ISSUE-NOV19.6-O129 fixed (user request). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.6-HUD).
- Tests: 984 unit; A15 `HudLayoutAuditTest` + `TextFitTest` 8/8, `SensorHudValuesTest` 5/5, `ProductionReadinessAuditTest` 3/3; release cold starts clean (Sensor and Monitor, A15). Live on the A15: the Sensor line and both Monitor lines are one line each.
- `HudStateTest`: the icon test was rewritten as `valuesCarryOnlyTheirUnitSigns` (same check, new format) and `largeAccuracyDropsItsDecimal` added; no check removed.
- Not run: 233.3 on the S21FE (not connected).

**Audit line**: Nov19.6-HUD: [SOT Count: 1045 (Rules: 545), Open: H:0, M:2, L:10, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 487), QA: 167]

## 🎯 Next Resumption Focus
1. S21FE checks: install release Monitor Nov19.6-HUD, then 233.3 (one HUD line), 230.3 (GPS-Tracker saves), 228.2 (overlay X), 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small ideas: SIMP-NOV19.5-S1 (leaner HudState, deferred by the user), SIMP-NOV18.12-S1, SIMP-NOV6.3-S1.

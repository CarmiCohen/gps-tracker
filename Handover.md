```
git add -A
git commit -m "Nov22.6 - THREE FOLLOW MODES (SIMP-NOV22.5-S1)

- SIMP-NOV22.5-S1: MapFollowMode.NONE, never set since the Map icon went, removed; TRACKER, VIEWER, AUTO remain.
- MAP_FUNCTIONALITY_SPEC names the modes TRACKER and VIEWER. Chapter 239, SOT 1.478.
- Tests: 1039 unit; A15 ProductionReadinessAuditTest 3/3; release cold starts clean.
- Version Nov22.6 (156)."
git tag Nov22.6
git push origin main --tags
```

# Project Handover - Version Nov22.6 (Three Follow Modes)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.6 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED on 2026-11-22 (`Service Suspended`, ISSUE-NOV22.5-O133, High). The user must restore it at the Render dashboard; until then no phone-to-phone test can run. Repo `relay-server` v6.047 (`1a04834`) unchanged.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/` (`install_release.ps1 -NoBuild`, `device_audit.cmd <serial> <package>` with the app running).
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. Light detection rise-only, on by default. Map: centers once per foreground; no Map icon (2026-11-22).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.6 running as `T` (`self=T` checked), no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). After a Sensor `am instrument` run, check `device_check.ps1 -App sensor -Command STATE` (`self=T`). A fresh Monitor asks for location: grant, then `device_check.ps1 -App monitor -Command PAIR -Id T`. Any alarm screen counts as leaving the foreground, so the map re-centers on return.

## 🟢 Status: PASSED (Nov22.6)
- SIMP-NOV22.5-S1 done. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.6).
- Tests: 1039 unit (no test added or removed); A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean (zero Davey, StackLog, fatal).
- Not run: device checks of the map (no behaviour change; the relay is suspended).

**Audit line**: Nov22.6: [SOT Count: 1051 (Rules: 551), Open: H:1, M:2, L:10, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 500), QA: 173]

## 🎯 Next Resumption Focus
1. O133: the user restores the relay on Render; then check a Sensor -> Monitor row (`/effort low`).
2. 238.2 on a Monitor with telemetry coming: a panned map stays for 30 s (`/effort low`).
3. 237.3 at the A15 (torch, `Light alert`) and the S21FE checks: release Monitor Nov22.6, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`).
4. O112 when the user has noted false alarms (`/effort high`); the user's pending decisions O91 and O26.
5. Small ideas: SIMP-NOV22.4-S1 (light on the rows, `/effort high`), SIMP-NOV22.4-S2, SIMP-NOV22.3-S1.

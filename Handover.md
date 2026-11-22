```
git add -A
git commit -m "Nov22.7 - THE MONITOR'S OWN ID (ISSUE-NOV22.7-O135, O134)

- O135: the Monitor ID is set in the Monitor's Setup dialog (not blank, not the Tracker ID); the session rejoins the relay under it and the own trail moves with it; pairing and settings import check it too.
- O135: every Monitor message to the Sensor carries 'from'; the Sensor shows the Monitor heard last (Setup dialog, Info, one event per Monitor per run). Relay unchanged.
- O134 opened (Medium, postponed by the user): two Monitors' Home Points contradict, they do not merge.
- Tests: 1047 unit; A15 MonitorIdTrailTest 2/2, ProductionReadinessAuditTest 3/3; release cold starts clean. Chapter 240, SOT 1.479.
- Version Nov22.7 (157)."
git tag Nov22.7
git push origin main --tags
```

# Project Handover - Version Nov22.7 (The Monitor's Own ID)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.7 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED (`Service Suspended`, ISSUE-NOV22.5-O133, High). The user must restore it at the Render dashboard; until then no phone-to-phone test can run. Repo `relay-server` v6.047 (`1a04834`) unchanged.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/` (`install_release.ps1 -NoBuild`, `device_audit.cmd <serial> <package>` with the app running). The IDE's live analysis is unsynced (every import unresolved); trust Gradle.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. Monitor ID (2026-11-22): the Sensor shows it, it does not obey only one Monitor; O134 (Home Points of two Monitors) postponed, to be discussed.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.7 running as `T` (`self=T` checked), no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). After a Sensor `am instrument` run, check `device_check.ps1 -App sensor -Command STATE` (`self=T`). A fresh Monitor asks for location: grant, then `device_check.ps1 -App monitor -Command PAIR -Id T`.

## 🟢 Status: PASSED (Nov22.7)
- ISSUE-NOV22.7-O135 done; ISSUE-NOV22.7-O134 opened. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.7).
- Tests: 1047 unit; A15 `MonitorIdTrailTest` 2/2, `ProductionReadinessAuditTest` 3/3; release cold starts clean (zero Davey, StackLog, fatal).
- Not run: 240.3 (Setup dialog edit and the Sensor showing the ID live): the relay is suspended and the S21FE not connected.

**Audit line**: Nov22.7: [SOT Count: 1052 (Rules: 552), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 503), QA: 174]

## 🎯 Next Resumption Focus
1. O133: the user restores the relay on Render; then a Sensor -> Monitor row, 238.2 and 240.3 (`/effort low`).
2. O134: discuss the Home Points of two Monitors with the user (recommended: one shared set the Sensor sends back) (`/effort high` to build).
3. 237.3 at the A15 (torch, `Light alert`) and the S21FE checks: release Monitor Nov22.7, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`).
4. O112 when the user has noted false alarms (`/effort high`); the user's pending decisions O91 and O26.
5. Small ideas: SIMP-NOV22.4-S1 (light on the rows, `/effort high`), SIMP-NOV22.4-S2, SIMP-NOV22.3-S1.

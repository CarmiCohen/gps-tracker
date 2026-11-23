```
git add -A
git commit -m "Nov23.1 - UNKNOWN AS NULL (SIMP-NOV17.7-S1)

- SIMP-NOV17.7-S1: LocationUpdate battery and temperature are null when not known (-1 is a real temperature); the row leaves batt and temp out, an old Sensor's batt -1 reads as null; ForensicBuffer version 7 stores an unknown temperature as NaN and upgrades the -1 of versions 5 and 6 to unknown.
- Readers check null: Monitor alarms, HUD and Info, Sensor graphs; the row clock no longer reads an unknown battery as critical.
- The Nov23.00 index hash, from the last session.
- Tests: 1089 unit (+6); A15 ProductionReadinessAuditTest 3/3; release Sensor start clean, buffer 6 -> 7 kept its sequence. Chapter 249, SOT 1.489.
- Version Nov23.1 (166)."
git tag Nov23.1
git push origin main --tags
```

# Project Handover - Version Nov23.1 (Unknown As Null)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.1 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED (O133, High, Work Queue row 73): after Render's monthly reset (probably 2026-12-01) the user runs Manual Deploy, Deploy latest commit (v6.048, `3c82603`), then 241.3, 238.2, 240.3, 244.3, 249.3 and the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s, longer than the tool's wait: run it in the background, or read `build/run_tests.log` then.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render; SIMP-NOV22.8-S1, S2 very low priority; the SIMP ideas one per version, not bundled.
- **Wire change**: a Monitor before Nov23.1 shows an unknown battery or temperature from a Nov23.1 Sensor as 0 (it read a missing `batt` and `temp` as 0); install both apps at Nov23.1 or later together.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor with the Nov23.1 code running as `T` (built before the version bump, so it still names itself Nov23.00), forensic buffer version 7, battery state real, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). For the 10 s rate on the A15 simulate `dumpsys battery unplug` and `set status 3`; restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov23.1)
- SIMP-NOV17.7-S1 done: battery and temperature are null when not known, from the Sensor through the row and the forensic buffer (version 7) to the Monitor. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov23.1).
- Tests: 1089 unit; A15 `ProductionReadinessAuditTest` 3/3; release Sensor start clean, its buffer upgraded 6 -> 7 with the sequence kept.
- Not run: the Monitor's cold start (not on the A15, S21FE not connected); 249.3 (live battery and temperature on a Monitor) waits for the relay.

**Audit line**: Nov23.1: [SOT Count: 1062 (Rules: 562), Open: H:2, M:3, L:10, Ideas: H:0, M:0, L:21, Testing: PASSED (Sub-items: 528), QA: 184]

## 🎯 Next Resumption Focus
1. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch) (`/effort low`).
2. O133: after Render's reset the user deploys relay v6.048; then 241.3, 238.2, 240.3, 244.3, 249.3 and the bandwidth (`/effort low`).
3. Without phones: O137 (forced-disconnection alarm, `/effort high`) when the user wants it before the relay is back, or the next SIMP idea (one per version, `/effort medium`).
4. O134: discuss the Home Points of two Monitors with the user (`/effort high` to build).
5. The S21FE checks: release Monitor Nov23.1, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`); O112 when the user has noted false alarms.

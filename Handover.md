```
git add -A
git commit -m "Nov23.4 - A QUIETER EVENTS LIST (ISSUE-NOV23.4-O139)

- ISSUE-NOV23.4-O139 (user request): one line per alarm on the Events list. The Monitor's Ended, Acknowledged and Home Point lines and the Sensor's Home points received are details (the latter still sent to the Monitor); a Sensor alarm names its kind (wire k), and one the Monitor answered with the same alarm (60 s before to 5 min after) is listed only with Details.
- The Nov23.2 index hash, from the last session; SIMP-NOV23.4-S1 added.
- Tests: 1127 unit; A15 ProductionReadinessAuditTest 3/3, release Sensor start clean. Chapter 251, SOT 1.491.
- Version Nov23.4 (168)."
git tag Nov23.4
git push origin main --tags
```

# Project Handover - Version Nov23.4 (A Quieter Events List)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.4 committed, tagged and pushed by Claude (Completion step 9); the index row is filled right after the push and left uncommitted for the next version. Nov23.3 was not used (SIMP-NOV19.6-S1 blocked by O72).
- **Relay**: still SUSPENDED (O133, High, Work Queue row 73): after Render's monthly reset (probably 2026-12-01) the user runs Manual Deploy, Deploy latest commit (v6.049, `8b88b42`), then 241.3, 238.2, 240.3, 244.3, 249.3, 250.3, 251.3 and the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background, or read `build/run_tests.log` then.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render; SIMP-NOV22.8-S1, S2 very low priority; the SIMP ideas one per version, not bundled. 2026-11-23: merging an alarm's Events lines rejected in favour of details (O139).
- **Wire change**: a Monitor before Nov23.1 shows an unknown battery or temperature as 0; install both apps at Nov23.1 or later together. A Monitor before Nov23.2 reads every silence cause as a plain silence. A Monitor before Nov23.4 ignores a Sensor event's kind and keeps listing the Sensor's alarms.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov23.4 running as `T`, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). For the 10 s rate on the A15 simulate `dumpsys battery unplug` and `set status 3`; restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov23.4)
- ISSUE-NOV23.4-O139 done: the Events list shows one line per alarm; ends, acknowledgements, Home Point changes and the Sensor alarms the Monitor answered appear only with Details. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov23.4).
- Tests: 1127 unit; A15 `ProductionReadinessAuditTest` 3/3; release Sensor start clean.
- Not run: the Monitor's cold start (not on the A15, S21FE not connected); 251.3 (the live list) waits for the relay and a paired Monitor. Events already kept keep their level.

**Audit line**: Nov23.4: [SOT Count: 1064 (Rules: 564), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 534), QA: 186]

## 🎯 Next Resumption Focus
1. Without phones: the next SIMP idea, SIMP-NOV15.5-S1 (SIMP-NOV19.6-S1 is blocked by the O72 toolchain upgrade).
2. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch).
3. O133: after Render's reset the user deploys relay v6.049; then 241.3, 238.2, 240.3, 244.3, 249.3, 250.3, 251.3 and the bandwidth.
4. O134: discuss the Home Points of two Monitors with the user.
5. The S21FE checks: release Monitor Nov23.4, then 237.4, 233.3, 230.3, 228.2, 227.4; O112 when the user has noted false alarms.

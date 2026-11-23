```
git add -A
git commit -m "Nov23.2 - FORCED DISCONNECTION ALARM (ISSUE-NOV22.10-O137)

- ISSUE-NOV22.10-O137: the relay's silence notice carries its SilenceCause: the Sensor's last word (owner STOP, service stopped, shutdown or empty battery, airplane mode, SIM removed on mobile data), closed (socket closed by the phone, 10 s), or silent; relay v6.049 (relay repo 8b88b42).
- Monitor: TRACKER_FORCED_OFFLINE (highest priority) for a forced cause or a silence within 5 min after a Sensor alarm; the narrowed 'Tracker no connection' otherwise; nothing after the owner's STOP, only battery low after an empty battery, neither while the Monitor's own relay or internet is down.
- The Nov23.1 index hash, from the last session.
- Tests: 1121 unit, relay 27/27; A15 airplane mode gave the last word; ProductionReadinessAuditTest 3/3; release Sensor start clean. Chapter 250, SOT 1.490.
- Version Nov23.2 (167)."
git tag Nov23.2
git push origin main --tags
```

# Project Handover - Version Nov23.2 (Forced Disconnection Alarm)

## ▶️ Resumption State (Read First)
- **Git**: Nov23.2 committed, tagged and pushed by Claude (Completion step 9), the relay repo's v6.049 (`8b88b42`) pushed too; the index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED (O133, High, Work Queue row 73): after Render's monthly reset (probably 2026-12-01) the user runs Manual Deploy, Deploy latest commit (now v6.049, `8b88b42`), then 241.3, 238.2, 240.3, 244.3, 249.3, 250.3 and the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about 100 s: run it in the background, or read `build/run_tests.log` then.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render; SIMP-NOV22.8-S1, S2 very low priority; the SIMP ideas one per version, not bundled. 2026-11-23: O137 and SIMP-NOV19.6-S1 (Nov23.3) in one session, at high effort.
- **Wire change**: a Monitor before Nov23.1 shows an unknown battery or temperature as 0; install both apps at Nov23.1 or later together. A Monitor before Nov23.2 reads every silence cause as a plain silence ("Tracker offline").
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor with the Nov23.2 code running as `T` (built before the version bump, so it names itself Nov23.1), airplane mode off, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). For the 10 s rate on the A15 simulate `dumpsys battery unplug` and `set status 3`; restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov23.2)
- ISSUE-NOV22.10-O137 done: the forced-disconnection alarm, from the Sensor's last word and the relay's closed socket to `TRACKER_FORCED_OFFLINE` on the Monitor. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov23.2).
- Tests: 1121 unit, relay 27/27; A15 airplane mode logged the last word (not sent, no relay); `ProductionReadinessAuditTest` 3/3; release Sensor start clean.
- Not run: the Monitor's cold start (not on the A15, S21FE not connected); 250.3 (the live alarms) waits for relay v6.049 and a paired Monitor. Limits (archive): a last word is best effort, the SIM broadcast was not observed, an OS kill or a Sensor update also counts as `closed`.

**Audit line**: Nov23.2: [SOT Count: 1063 (Rules: 563), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:21, Testing: PASSED (Sub-items: 531), QA: 185]

## 🎯 Next Resumption Focus
1. SIMP-NOV19.6-S1 (Compose's own text fitting) as Nov23.3, in this session (user decision 2026-11-23).
2. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch).
3. O133: after Render's reset the user deploys relay v6.049; then 241.3, 238.2, 240.3, 244.3, 249.3, 250.3 and the bandwidth.
4. O134: discuss the Home Points of two Monitors with the user.
5. The S21FE checks: release Monitor Nov23.2, then 237.4, 233.3, 230.3, 228.2, 227.4; O112 when the user has noted false alarms.

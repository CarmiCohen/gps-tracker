```
git add -A
git commit -m "Nov22.12 - ONE COUNTED EMIT (SIMP-NOV22.9-S1)

- SIMP-NOV22.9-S1: SocketioSignalingClient.emitCounted(event, payload) sends and counts every data event (ping_cmd, location_batch, violation_event, backfill_request, backfill_response, settings_update via emitSettings); join and push tokens stay uncounted. Behaviour unchanged.
- SIMP-NOV8.13-S2 note: sendViolation alone has no connected check.
- The Nov22.11 index hash, from the last session.
- Tests: 1066 unit (CountedEmitTest new); A15 ProductionReadinessAuditTest 3/3; release Sensor start clean. Chapter 245, SOT 1.485.
- Version Nov22.12 (162)."
git tag Nov22.12
git push origin main --tags
```

# Project Handover - Version Nov22.12 (One Counted Emit)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.12 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED (O133, High, Work Queue row 73): after Render's monthly reset (probably 2026-12-01) the user runs Manual Deploy, Deploy latest commit (v6.048, `3c82603`), then 241.3, 238.2, 240.3, 244.3 and the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render; SIMP-NOV22.8-S1, S2 very low priority; the SIMP ideas one per version, not bundled.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.12 running as `T`, forensic buffer version 6, battery state real, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). For the 10 s rate on the A15 simulate `dumpsys battery unplug` and `set status 3`; restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov22.12)
- SIMP-NOV22.9-S1 done: one counted sender for every data event. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.12).
- Tests: 1066 unit; A15 `ProductionReadinessAuditTest` 3/3; release Sensor start clean.
- Not run: the Monitor's cold start (not on the A15, S21FE not connected); a live data count (relay suspended).

**Audit line**: Nov22.12: [SOT Count: 1058 (Rules: 558), Open: H:2, M:3, L:10, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 516), QA: 179]

## 🎯 Next Resumption Focus
1. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch) (`/effort low`).
2. O133: after Render's reset the user deploys relay v6.048; then 241.3, 238.2, 240.3, 244.3 and the bandwidth (`/effort low`).
3. Without phones: SIMP-NOV22.10-S1 (one learned background, `/effort high`), then SIMP-NOV22.11-S1 (a leaner SensorSnapshot, `/effort medium`); O137 (forced-disconnection alarm, `/effort high`) when the user wants it before the relay is back.
4. O134: discuss the Home Points of two Monitors with the user (`/effort high` to build).
5. The S21FE checks: release Monitor Nov22.12, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`); O112 when the user has noted false alarms.

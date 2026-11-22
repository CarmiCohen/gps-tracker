```
git add -A
git commit -m "Nov22.13 - ONE LEARNED BACKGROUND (SIMP-NOV22.10-S1)

- SIMP-NOV22.10-S1: core-shared LearnedBackground (3 s warm-up, time constants 0.5 s, 1 s fall, 10 s rise, fed spans) is the one background of AcousticDetector and LightDetector; BACKGROUND_WARM_UP_MS replaces the two warm-up constants. The noise now uses the light's round constants (per frame 0.181/0.095/0.00995 for 0.2/0.1/0.01).
- New idea SIMP-NOV22.13-S1 (one rise detector).
- The Nov22.12 index hash, from the last session.
- Tests: 1073 unit (LearnedBackgroundTest new); A15 ProductionReadinessAuditTest 3/3; release Sensor start clean. Chapter 246, SOT 1.486.
- Version Nov22.13 (163)."
git tag Nov22.13
git push origin main --tags
```

# Project Handover - Version Nov22.13 (One Learned Background)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.13 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED (O133, High, Work Queue row 73): after Render's monthly reset (probably 2026-12-01) the user runs Manual Deploy, Deploy latest commit (v6.048, `3c82603`), then 241.3, 238.2, 240.3, 244.3 and the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle. `run_tests.ps1` takes about a minute but can outlast the tool's wait: read `build/run_tests.log` then.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render; SIMP-NOV22.8-S1, S2 very low priority; the SIMP ideas one per version, not bundled.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.13 running as `T`, forensic buffer version 6, battery state real, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). For the 10 s rate on the A15 simulate `dumpsys battery unplug` and `set status 3`; restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov22.13)
- SIMP-NOV22.10-S1 done: one learned background for the noise and the light. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.13).
- Tests: 1073 unit; A15 `ProductionReadinessAuditTest` 3/3; release Sensor start clean.
- Not run: the Monitor's cold start (not on the A15, S21FE not connected); a live noise or light alert (241.4, 237.3 wait for the user at the A15).

**Audit line**: Nov22.13: [SOT Count: 1059 (Rules: 559), Open: H:2, M:3, L:10, Ideas: H:0, M:0, L:25, Testing: PASSED (Sub-items: 518), QA: 180]

## 🎯 Next Resumption Focus
1. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch) (`/effort low`).
2. O133: after Render's reset the user deploys relay v6.048; then 241.3, 238.2, 240.3, 244.3 and the bandwidth (`/effort low`).
3. Without phones: SIMP-NOV22.11-S1 (a leaner SensorSnapshot, `/effort medium`), then SIMP-NOV22.13-S1 (one rise detector, `/effort medium`); O137 (forced-disconnection alarm, `/effort high`) when the user wants it before the relay is back.
4. O134: discuss the Home Points of two Monitors with the user (`/effort high` to build).
5. The S21FE checks: release Monitor Nov22.13, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`); O112 when the user has noted false alarms.

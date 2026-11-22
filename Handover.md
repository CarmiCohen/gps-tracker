```
git add -A
git commit -m "Nov22.9 - ONE SETTINGS SENDER (SIMP-NOV22.3-S1)

- SIMP-NOV22.3-S1: the nine settings messages of SocketioSignalingClient leave through one emitSettings (connected check, emit, data count), with emitFromMonitor (Monitor ID stamp) and emitFromSensor (Sensor ID); behaviour unchanged.
- SIMP-NOV22.9-S1 logged: one counted emit for the other events.
- Queue row 73 (O133: bring the relay back after Render's reset) and the Nov22.8 index hash, from the last session.
- Tests: 1058 unit (SettingsSenderTest new, 2); A15 ProductionReadinessAuditTest 3/3; release cold starts clean. Chapter 242, SOT 1.482.
- Version Nov22.9 (159)."
git tag Nov22.9
git push origin main --tags
```

# Project Handover - Version Nov22.9 (One Settings Sender)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.9 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version.
- **Relay**: still SUSPENDED (O133, High, Work Queue row 73): after Render's monthly reset (probably 2026-12-01) the user runs Manual Deploy, Deploy latest commit (v6.048, `3c82603`), then 241.3, 238.2, 240.3 and the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render; SIMP-NOV22.8-S1, S2 very low priority; the SIMP ideas one per version, not bundled.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.9 running as `T`, battery state real, no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). For the 10 s rate on the A15 simulate `dumpsys battery unplug` and `set status 3`; restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov22.9)
- SIMP-NOV22.3-S1 done: one settings sender. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.9).
- Tests: 1058 unit; A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean.
- Not run on phones: the relay is down and the S21FE not connected; the settings path is covered by `SettingsSenderTest`.

**Audit line**: Nov22.9: [SOT Count: 1055 (Rules: 555), Open: H:1, M:3, L:9, Ideas: H:0, M:0, L:26, Testing: PASSED (Sub-items: 509), QA: 176]

## 🎯 Next Resumption Focus
1. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch) (`/effort low`).
2. O133: after Render's reset the user deploys relay v6.048; then 241.3, 238.2, 240.3 and the bandwidth (`/effort low`).
3. Without phones: SIMP-NOV22.4-S2 (one rise confirmation for noise and light, `/effort high`), then SIMP-NOV22.4-S1 (light on the rows, `/effort medium`), then SIMP-NOV22.9-S1.
4. O134: discuss the Home Points of two Monitors with the user (`/effort high` to build).
5. The S21FE checks: release Monitor Nov22.9, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`); O112 when the user has noted false alarms.

```
git add -A
git commit -m "Nov22.8 - ONE ROW A SECOND, THE ALARM WINDOW (ISSUE-NOV22.5-O133, O136)

- O133: the relay's 5 GB Render bandwidth went on 10 rows a second from the plugged-in A15, each sent back to the Sensor too; the row clock is now 1 s at most (10 s stationary or critical).
- O133: relay v6.048 (3c82603, not deployed): a batch as one message, never back to the sender; viewer status on change only; cache every 5 s (closes O10, SIMP-NOV6.4-S1).
- O136 (user request): one row a second for 5 min after the last alarm of any kind; critical Sensor battery or heat keeps 10 s.
- SIMP-NOV22.8-S1, S2 logged as very low priority (user decision 2026-11-22).
- Tests: 1054 unit, relay 20/20; A15 11/11 instrumented, live 1 s and 10 s rates; release cold starts clean. Chapter 241, SOT 1.480, 1.481.
- Version Nov22.8 (158)."
git tag Nov22.8
git push origin main --tags
```

# Project Handover - Version Nov22.8 (One Row A Second, The Alarm Window)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.8 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version. Relay repo v6.048 pushed (`3c82603`).
- **Relay**: still SUSPENDED (O133, High): Render's 5 GB free monthly bandwidth was used up (cause fixed in Nov22.8). After the monthly reset (probably the 1st) the user runs Manual Deploy, Deploy latest commit (v6.048), then 241.3 and a look at the bandwidth graph after a few days.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. Scripts are in `TESTS/`. The IDE's live analysis is unsynced; trust Gradle.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred. O134 postponed, to be discussed. 2026-11-22: stay on Render for now; SIMP-NOV22.8-S1 (fewer rows while unwatched) and S2 (Monitor foreground state) very low priority.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.8 running as `T` (`self=T` checked), battery state real (`dumpsys battery reset`), no Monitor. S21FE (`R5CRC14PG4F`): not connected; release Monitor Nov19.5, paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with the `HiltTestRunner` of each app (Operational Notes). To test the 10 s rate on the A15 simulate both `dumpsys battery unplug` and `set status 3` (FULL counts as charging); restore with `dumpsys battery reset`.

## 🟢 Status: PASSED (Nov22.8)
- ISSUE-NOV22.5-O133 cause found and cut (stays open until the relay runs again); ISSUE-NOV22.8-O136 done. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.8).
- Tests: 1054 unit, relay 20/20; A15 11/11 instrumented; live 1000 ms on USB and 10000 ms on simulated battery; release cold starts clean.
- Not run: 241.3 (relay down) and 241.4 (a knock on the A15 for the alarm window; the user was away, postponed).

**Audit line**: Nov22.8: [SOT Count: 1054 (Rules: 554), Open: H:1, M:3, L:9, Ideas: H:0, M:0, L:26, Testing: PASSED (Sub-items: 507), QA: 175]

## 🎯 Next Resumption Focus
1. 241.4 with the user at the A15 (simulate battery, knock, expect `Alarm window: true` and 1000 ms, 10000 ms 5 min later), with 237.3 (torch) (`/effort low`).
2. O133: after Render's reset the user deploys relay v6.048; then 241.3, 238.2, 240.3 and the Render bandwidth after a few days (`/effort low`).
3. O134: discuss the Home Points of two Monitors with the user (`/effort high` to build).
4. The S21FE checks: release Monitor Nov22.8, then 237.4, 233.3, 230.3, 228.2, 227.4 (`/effort low`).
5. O112 when the user has noted false alarms; small ideas SIMP-NOV22.4-S1, S2, SIMP-NOV22.3-S1.

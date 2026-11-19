```
git add -A
git commit -m "Nov19.0 - O19 AND O86 CLOSED, MONITOR BATTERY CHECK OPENED (ISSUE-NOV5.4-O19, NOV10.8-O86, NOV19.0-O122)

- O19: the archive repos were already clean; nothing left to restore.
- O86: closed, the permanent connection stays (user decision 2026-11-19).
- O122: measure the Monitor's battery use once the project is complete.
- 226.3 passed on the S21FE; 227.4 not run (the Monitor did not freeze).
- Index row Nov18.12 (9be92fb).
- Tests: :core-shared:test (docs-only version).
- Version Nov19.0 (144)."
git tag Nov19.0
git push origin main --tags
```

# Project Handover - Version Nov19.0 (O19 And O86 Closed, Monitor Battery Check Opened)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.0 waits for the Git block above; the index row Nov19.0 says `pending` (the next session fills it from the tag). Nov18.12 is `9be92fb`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` does not build: run `assembleRelease` first (SIMP-NOV18.11-S1). Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. 2026-11-19: O86 closed with the permanent connection kept; O122 (Monitor battery) only once the project is otherwise complete.
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.12, service running, battery exemption in place, airplane off. S21FE (`R5CRC14PG4F`): disconnected; release Monitor Nov18.12 (143) installed fresh this session (it had been uninstalled), pairing V -> T restored by Auto Backup, no battery exemption; the user restores Global Mute.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). On the S21FE, `am freeze` does not freeze the Monitor (foreground service), and the Monitor kept its socket through 10 min of screen-off.

## 🟢 Status: PASSED (Nov19.0)
- O19 and O86 closed, O122 opened (Work Queue row 72). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.0).
- 226.3 passed on the S21FE (10 s drop: no alarm; 40 s drop: `Relay disconnected` after 30.2 s).
- Not run: 227.4 (a frozen Monitor, push only): the S21FE Monitor never froze.
- Tests: `:core-shared:test` (docs-only version, D5).

**Audit line**: Nov19.0: [SOT Count: 1039 (Rules: 539), Open: H:0, M:2, L:12, Ideas: H:0, M:0, L:26, Testing: PASSED (Sub-items: 471), QA: 161]

## 🎯 Next Resumption Focus
1. Fill the index row Nov19.0 from the tag (`/effort low`).
2. 227.4 when the S21FE can stay connected and idle with the screen off for an hour or more; watch its `REV_Signaling` lines stop, then take the A15 Sensor's exemption away (`/effort low`).
3. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
4. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).

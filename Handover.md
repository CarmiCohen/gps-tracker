```
git add -A
git commit -m "Nov22.3 - ONE SETTINGS DECODER (SIMP-NOV15.3-S1)

- SIMP-NOV15.3-S1: core-network SettingsWire.decode reads the settings_relay type once into one sealed SettingsMessage;
  the client's listener dispatches once instead of running nine decoders; InfoRequestWire.decode added.
- SettingsWireTest (chapter 236, SOT 1.475); new idea SIMP-NOV22.3-S1; index row Nov22.1 (2d0c430).
- Tests: 1002 unit; A15 ProductionReadinessAuditTest 3/3; release Sensor and Monitor cold starts clean on the A15.
- Version Nov22.3 (153)."
git tag Nov22.3
git push origin main --tags
```

# Project Handover - Version Nov22.3 (One Settings Decoder)

## ▶️ Resumption State (Read First)
- **Git**: Nov22.3 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for the next version. Nov22.2 was not used (version name given by the user).
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`; unchanged (wire format unchanged).
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` builds first (`-NoBuild` to skip, `-Apps sensor` for one app). `device_audit.cmd <serial> <package>` needs the app running. Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. SIMP-NOV19.5-S1 deferred by the user (2026-11-19).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov22.3, running as `T`, no Monitor (installed for the audit, uninstalled after). S21FE (`R5CRC14PG4F`): not connected this session; release Monitor Nov19.5 (149), paired V -> T.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). After a Sensor `am instrument` run, check its ID with `device_check.ps1 -App sensor -Command STATE` (`self=T`). The Sensor console opens on a "System Idle" lock screen: one tap unlocks it.

## 🟢 Status: PASSED (Nov22.3)
- SIMP-NOV15.3-S1 done (the S21FE was not connected). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov22.3).
- Tests: 1002 unit (new `SettingsWireTest`; no test removed); A15 `ProductionReadinessAuditTest` 3/3; release cold starts clean (Sensor 708 ms, Monitor 594 ms, A15).
- Not run: a live settings exchange between two phones (the S21FE was not connected); the client paths are covered by the existing wire tests on the fake socket.

**Audit line**: Nov22.3: [SOT Count: 1048 (Rules: 548), Open: H:0, M:2, L:10, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 492), QA: 170]

## 🎯 Next Resumption Focus
1. S21FE checks: install release Monitor Nov22.3, then 233.3 (one HUD line), 230.3 (GPS-Tracker saves), 228.2 (overlay X), 227.4 after a long natural idle; watch one `REV_Push` line (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small ideas: SIMP-NOV22.3-S1 (one settings sender, the sending side of SettingsWire, `/effort medium`), SIMP-NOV6.1-S1 (shared device status), SIMP-NOV19.5-S1 (deferred by the user).

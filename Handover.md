```
git add -A
git commit -m "Nov19.5 - THREE-COLOR LEDS, GPS MEANS SATELLITES, SPREAD MENU ICONS (ISSUE-NOV19.5-O127, O128)

- O127: HUD LEDs green OK, red not OK, gray no info; own lines INT/SRV/GPS/TRK or VWR, the Monitor's Sensor line GPS only; DAT and WDG gone; GPS = fix from 4+ satellites (gray after 2 min).
- O128: menu bar icons share the bar width as 40 dp tap areas; the motion label stays whole on the S21FE.
- install_release.ps1 works with one app; index row Nov19.4 (8d25572).
- Tests: 982 unit; A15 HudLayoutAuditTest + TextFitTest 8/8, ProductionReadinessAuditTest 3/3; release Sensor (A15) and Monitor (S21FE) cold starts clean.
- Version Nov19.5 (149)."
git tag Nov19.5
git push origin main --tags
```

# Project Handover - Version Nov19.5 (Three-Color LEDs, GPS Means Satellites, Spread Menu Icons)

## ▶️ Resumption State (Read First)
- **Git**: Nov19.5 committed, tagged and pushed by Claude (Completion step 9); its index row is filled right after the push and left uncommitted for Nov19.6.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`), deployed, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` builds first (`-NoBuild` to skip; `-Apps sensor` works since Nov19.5). `device_audit.cmd <serial> <package>` needs the app running (cold start with `monkey -p <package> -c android.intent.category.LAUNCHER 1`). Scripts are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the main and relay repos; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; `//` means "continue". O112: the user notes false alarms as they happen. O122 only once the project is otherwise complete. 2026-11-19: LEDs green/red/gray, GPS = 4+ satellites, gray after 2 min, the Monitor's Sensor line GPS only (O127).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov19.5, running as `T` (restored after the user removed it: grants, listener, battery exemption, ID), no Monitor. S21FE (`R5CRC14PG4F`): release Monitor Nov19.5 (149), paired V -> T; mute and pairing untouched.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` (Operational Notes). A Sensor network cut makes the S21FE Monitor alarm with the siren: do not cut while the S21FE is the user's phone in use.

## 🟢 Status: PASSED (Nov19.5)
- ISSUE-NOV19.5-O127 and O128 fixed (user requests); `install_release.ps1` one-app fix. Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov19.5).
- Tests: 982 unit (known kapt warning, O72); A15 `HudLayoutAuditTest` + `TextFitTest` 8/8, `ProductionReadinessAuditTest` 3/3; release cold starts clean (Sensor A15, Monitor S21FE). Live: Sensor network cut (INT, SRV red, VWR gray), location off (GPS red), S21FE lines and bar as designed; GNSS status about 1 Hz on a still Monitor.
- Not run: TRK red and gray live on the S21FE (siren on the personal phone); unit-tested.

**Audit line**: Nov19.5: [SOT Count: 1044 (Rules: 544), Open: H:0, M:2, L:10, Ideas: H:0, M:0, L:24, Testing: PASSED (Sub-items: 484), QA: 166]

## 🎯 Next Resumption Focus
1. S21FE checks: 230.3 (GPS-Tracker saves), 228.2 (overlay X), 227.4 after a long natural idle (`/effort low`).
2. The 225.3 noise line when a noise alert comes; O112 when the user has noted false alarms (`/effort high`).
3. The user's pending decisions: O91 (RTL fix choice), O26 (API 34 image).
4. Small ideas: SIMP-NOV19.5-S1 (leaner HudState), SIMP-NOV18.12-S1, SIMP-NOV6.3-S1.

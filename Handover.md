```
git add .
git commit -m "Nov9.3-MonitorStart - STARTUP FRAMES AUDITED ON RELEASE BUILDS (ISSUE-NOV8.13-O55)

- O55: trace (atrace + simpleperf) shows the Monitor's 726 ms first frame is ART's interpreter in the debug build; release starts in about 0.45 s, 0 Davey.
- New TESTS/install_release.ps1 (release signed with the debug key, data kept); Completion step 1 audits frames on it, then installDebug restores.
- ISSUE-NOV9.3-O63 fixed: AppVersionTest accepted no suffixed version name. New gap ISSUE-NOV9.3-O64 (MapJitterAuditTest changes the real pairing).
- Index row Nov9.2-Vibration filled (7a26409). SOT 1.403, chapter 165, ReleaseAuditBuildTest.
- TESTS: 537 unit passed (--rerun-tasks, twice), instrumented Sensor 42/42 Monitor 48/48, reciprocal simulation passed, release audit clean.
- VERSION: Nov9.3-MonitorStart, versionCode 73."
git tag Nov9.3-MonitorStart
git push origin main --tags
```

# Project Handover - Version Nov9.3-MonitorStart (Startup Frames Audited On Release Builds)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov9.3-MonitorStart^{commit}`.
- **User decisions (2026-11-09)**: ISSUE-NOV8.13-O55 as `Nov9.3-MonitorStart`; startup frames are audited on the release builds (`TESTS/install_release.ps1`, Completion step 1). `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`) has the Nov9.3 debug builds; Sensor `T` running, listening; Monitor `V` paired to `T`, 2 Home Points, radius 140. S21FE in personal use; never test on it.
- **Test tips**: `DOCS/issues.md` Operational Notes (debug `PAIR` while paired needs `UNPAIR` first; startup frames on release builds); `TESTS/TEST_GUIDELINES.md` section 4. Runners: `com.jd.app.sensor.HiltTestRunner`, `com.jd.app.monitor.HiltTestRunner` (`am instrument` keeps app data).

## 🟢 Status: PASSED (Nov9.3-MonitorStart)
- ISSUE-NOV8.13-O55 resolved (the debug build's interpreter, not app code); ISSUE-NOV9.3-O63 found and fixed; new gap ISSUE-NOV9.3-O64. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 537 unit test results, 0 failures; builds clean apart from known warnings (46 lines on a full recompile, all existing code). Instrumented on the A15: Sensor 42/42, Monitor 48/48.
- Reciprocal simulation (one phone): virtual viewer vs Sensor `T` and virtual tracker `SIM9` vs Monitor passed. Release audit: Sensor 388 ms, Monitor 457 ms, 0 Davey, StackLog, fatal.
- **Not run**: chapters 161.4, 162.4, 164.4 and ISSUE-NOV9.2-O62 need a person at the A15.

**Audit line**: Nov9.3-MonitorStart: [SOT Count: 977 (Rules: 477), Open: H:1, M:2, L:12, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 318), QA: 99]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Next to the A15: chapters 161.4, 162.4, 164.4, and a call for ISSUE-NOV9.2-O62; then close ISSUE-NOV5.2-O11 (`/effort low`).
3. Otherwise item 46, ISSUE-NOV8.13-O56: a decision on unique Sensor IDs (`/effort medium`).
4. Then the Work Queue in `DOCS/issues.md` (item 55, ISSUE-NOV9.3-O64, is a small test fix).

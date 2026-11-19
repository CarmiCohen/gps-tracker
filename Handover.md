```
git add -A
git commit -m "Nov18.12 - THE SENSOR PHONE WARNINGS BY PUSH (ISSUE-NOV10.8-O86)

- O86: relay v6.047 pushes the Sensor's setup report when what it lacks changes.
- O86: the Monitor notifies each new warning once, no siren; removed when fixed.
- O86: the Sensor sends a changed report also with no Monitor online.
- Index row Nov18.11 (4d5532a); idea SIMP-NOV18.12-S1.
- Tests: 956 unit, relay 18; SensorSetupNoticeDeviceTest (3) and
  ProductionReadinessAuditTest (3) on the A15; audits clean.
- Version Nov18.12 (143)."
git tag Nov18.12
git push origin main --tags
```

# Project Handover - Version Nov18.12 (The Sensor Phone Warnings By Push)

## ▶️ Resumption State (Read First)
- **Git**: Nov18.12 waits for the Git block above; the index row Nov18.12 says `pending` (the next session fills it from the tag). Nov18.11 is `4d5532a`.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.047 (commit `1a04834`, pushed); deployed by the user's Manual Deploy on 2026-11-18, `push: on`.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null` or with unresolved references in files not changed, run `gradlew --stop` and `gradlew clean`. `install_release.ps1` does not build: run `assembleRelease` first (SIMP-NOV18.11-S1). `run_tests.ps1`, `install_release.ps1`, `device_audit.cmd`, `audit_line.ps1 -Version <v>` are in `TESTS/`.
- **User decisions**: Rule 17: never remove a test without the user's explicit confirmation. Claude pushes the relay repo; with both phones connected run the Two-phone test without asking, checking the S21FE's foreground app first; airplane mode or Global Mute off for tests, restored after; `//` means "continue". O112: the user notes new false alarms as they happen. 2026-11-18: a new Sensor phone warning is a plain notification, never the siren (O86).
- **Phones**: A15 (`R58X40GV2AR`): debug Sensor Nov18.12, service running, no Monitor or test apks, airplane off, battery exemption in place. S21FE (`R5CRC14PG4F`): left adb this session; still release Monitor Nov18.10 (141), paired V -> T, which ignores the new `setup` push. The release Monitor is debug-key signed: the debug build installs over it with `install -r -d` and keeps the pairing.
- **Test tips**: never `connectedDebugAndroidTest`; use `am instrument` with `<pkg>.test/<pkg>.HiltTestRunner`, airplane mode on for Monitor runs (O90). With two phones, read a screen with `adb -s <serial> shell uiautomator dump`. Cutting the Monitor's own network raises Local internet lost with the siren (Operational Notes).

## 🟢 Status: PASSED (Nov18.12)
- O86's O83 warnings by push done (relay push, Monitor notification, Sensor sends with no Monitor online). Details: `STATUS/RESOLUTION_ARCHIVE.md` (Nov18.12).
- 227.3 passed on the A15 with both apps (relay +0.26 s, push +0.43 s, one notification, removed when fixed).
- Tests: `run_tests.ps1` 956 passed (known kapt warning, O72); relay `npm test` 18; A15: `SensorSetupNoticeDeviceTest` 3, `ProductionReadinessAuditTest` 3; release cold starts clean.
- Not run: 227.4 (a frozen Monitor, by push only) and 226.3 (a live relay drop): both need the S21FE with release Monitor Nov18.12.

**Audit line**: Nov18.12: [SOT Count: 1039 (Rules: 539), Open: H:0, M:3, L:12, Ideas: H:0, M:0, L:26, Testing: PASSED (Sub-items: 471), QA: 161]

## 🎯 Next Resumption Focus
1. Fill the index row Nov18.12 from the tag (`/effort low`).
2. Release Monitor Nov18.12 on the S21FE (`assembleRelease`, then `install_release.ps1 -Serial R5CRC14PG4F -Apps monitor`), then 227.4, 226.3 and the 225.3 noise line when a noise alert comes (`/effort low`).
3. O112 when the user has noted false alarms (`/effort high`); O86's last part (no permanent connection) waits for a free always-awake relay.
4. The user's pending decisions: O91 (RTL fix choice), O19 (`git restore`), O26 (API 34 image).

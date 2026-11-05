git add .
git commit -m "Nov5.1 - DEFAULT RELAY CORRECTION & PRODUCTION VERIFICATION

- ISSUE-NOV5.0-O1 / GAP-REVOL70.0-G1 RELAY: Corrected the Nov5.0 outage finding. Both apps defaulted to the wrong host; all defaults now come from RelayConfig.DEFAULT_URL (https://gps-survival-relay.onrender.com). Relay fixes that had never been deployed were published as v6.043 (commit 9a20990 in gps-survival-relay) and auto-deployed by Render.
- ISSUE-NOV5.0-O8 / GAP-REVOL70.0-G2 LIVE: Verified the final build through the production relay with Sensor on A15 and Monitor on S21FE: all LEDs active, real battery, live AGE, real map position, and the relay URL hot switch without an app restart.
- TESTS: Added RelayConfigTest. 103 of 103 automated tests pass, including 47 on the A15.
- DOCS: Added Chapter 119, Rule 1.354, Chapter 5.62, open gaps ISSUE-NOV5.1-O9 and O10, and baseline STATUS/BASELINE/Nov5.1/. Corrected the relay address in Chapters 105.2 and 107.1 and in 12_UX_AND_DEPLOYMENT.md.
- VERSION: Nov5.1, versionCode 32 (monitor) and 34 (sensor)."
git tag Nov5.1
git push origin main --tags

# Project Handover - Version Nov5.1 (Default Relay Correction & Production Verification)

## 🟢 Status: STABILIZED & VERIFIED ON PRODUCTION RELAY (Nov5.1)

Version Nov5.1 delivers these changes:
1. **Relay address**: Both apps now use `https://gps-survival-relay.onrender.com`, from the single constant `RelayConfig.DEFAULT_URL`.
2. **Relay server**: v6.043 is deployed (`9a20990` in `CarmiCohen/gps-survival-relay`). It adds secret rooms, `location_batch`, viewer status, and violation relay.
3. **Live tracking**: Sensor to Monitor tracking works through the production relay on the final build.
4. **Correction**: The Nov5.0 "production relay outage" was a wrong URL plus undeployed relay code, not an external outage.

**Resumption Token**: STABILIZED-PRODUCTION-RELAY-NOV5.1

---

## 🎯 Next Resumption Focus: Manual physical chapters and stress protocol (ISSUE-NOV5.0-O4)

1. Execute the physical chapters and `TEST_FORENSIC_STRESS_PROTOCOL.md` sections listed in `TESTS/test_results.md`.
2. Decide whether the 100ms telemetry interval is intended (ISSUE-NOV5.0-O3).
3. Observe recovery after an idle relay cold start (ISSUE-NOV5.1-O9).
4. Replace the hardcoded Monitor local HUD values (ISSUE-NOV5.0-O2).

### 🛡️ Implementation Status (Nov5.1)
- **Version Increment**: ✅ **VERIFIED** (`Nov5.1`, `versionCode` 32 monitor, 34 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (103/103; instrumented tests pinned to A15).
- **Production Two-Device Run**: ✅ **PASSED** on the final build.
- **Idle Cold-Start Recovery**: ⚠️ **NOT OBSERVED** (ISSUE-NOV5.1-O9).
- **Manual Physical Chapters & Stress Protocol**: ❌ **NOT EXECUTED** (see `TESTS/test_results.md`).

---

## 📂 Key Implementation Files
- `core-shared/.../RelayConfig.kt` (new): default relay address and saved-URL rule.
- `app-sensor/.../SensorViewModel.kt`, `SensorService.kt`, `AdminActivity.kt`: use `RelayConfig`.
- `app-monitor/.../MonitorViewModel.kt`, `MonitorSessionService.kt`, `MonitorActivity.kt`: use `RelayConfig`.
- `core-shared/src/test/.../RelayConfigTest.kt` (new).
- `relay-server/index.js`, `.gitignore`: separate repository; v6.043 is committed, pushed, and deployed.

---

## ⚠️ Operational Notes
- `relay-server` is a separate Git repository. Changes reach production only after they are pushed; Render then auto-deploys and restarts the relay.
- Never commit `relay-server/device_cache.json`; it can hold location data and is now ignored.
- An install that saved the old relay address keeps it until the URL is re-entered.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- The relay persists `device_cache.json` and replays it to clients, so do not send simulated positions to the production relay.
- After a test run the Sensor app needs its service started again from `START SENSOR SERVICE`.

---

## ✅ Verified Milestones (Nov5.1)
1. **Automated Suite**: 103/103 on the final code.
2. **Hardware**: Live A15 to S21FE telemetry through the production relay, with zero Davey or StackLog from either app process.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov5.1/`; tag `Nov5.1` is applied by the Git block above.

**Handover complete. System state locked at Nov5.1.**

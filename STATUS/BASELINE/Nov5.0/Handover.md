git add .
git commit -m "Nov5.0 - HUD DIRECTION, REAL-POSITION MAP CENTERING & TELEMETRY INTEGRITY

- GAP-REVOL69.0-G1 HUD: Added compact HudStatusBar spacing on the Monitor (root cause: inherited 24sp bodyLarge line height) and forced left-to-right layout and text direction in both apps.
- GAP-REVOL69.0-G2 MAP: Removed the hardcoded San Francisco fallback. The Monitor now requests location permission and reads its own position through MonitorLocationSource; resolveMapCenter selects the center; the world view shows until a real position exists; ACCESS_COARSE_LOCATION added.
- GAP-REVOL69.0-G3 TELEMETRY (CRITICAL): Fixed normal updates reaching the relay zeroed. SensorService released pooled LocationUpdate objects still held by SocketioSignalingClient; the client now keeps LocationUpdate.snapshot() copies. Added accuracy, speed, battery, charging, and temperature to the wire format, including backfill.
- GAP-REVOL69.0-G4 VWR: Added the missing viewer_status_relay listener, so the Sensor VWR LED can activate.
- GAP-REVOL69.0-G5 RELAY: connect() now replaces the session on a URL or identity change instead of ignoring it or leaking the old socket.
- GAP-REVOL69.0-G6 TRAIL: knownPositions removes (0,0) rows from the polyline, marker, and map center.
- GAP-REVOL69.0-G7 TESTS: Fixed the Monitor instrumented suite, which had not compiled since Nov4.37.
- TESTS: Added MapCenterResolverTest, HudLayoutAuditTest, and new SignalingBatchTest cases. 100 of 100 automated tests pass, including 47 on the A15.
- LIVE: Two-device run with Sensor on A15 and Monitor on S21FE through a local relay, because the production relay was unresponsive (ISSUE-NOV5.0-O1).
- DOCS: Added Chapter 118 to TEST_PROCEDURE.md, Rules 1.350 to 1.353, Chapter 5.61, open gaps ISSUE-NOV5.0-O1 to O7, and baseline STATUS/BASELINE/Nov5.0/.
- VERSION: Nov5.0, versionCode 31 (monitor) and 33 (sensor)."
git tag Nov5.0
git push origin main --tags

# Project Handover - Version Nov5.0 (HUD Direction, Real-Position Centering & Telemetry Integrity)

## 🟡 Status: AUTOMATED SUITE PASSED, PRODUCTION RELAY DOWN (Nov5.0)

Version Nov5.0 delivers these changes:
1. **HUD**: Compact line spacing on the Monitor, and forced left-to-right in both apps.
2. **Map**: The Monitor centers on real device positions; the hardcoded California location is gone.
3. **Telemetry (Critical)**: Sensor data now reaches the Monitor. Normal updates previously arrived zeroed, so earlier builds could not show a live tracker.
4. **Peer status**: The Sensor `VWR` LED now works, and relay URL changes apply without an app restart.
5. **Tests**: 100/100 automated tests pass on the final code, including the Monitor instrumented suite, which had not compiled since Nov4.37.

**Resumption Token**: NOV5.0-RELAY-RESTORE-PENDING

---

## 🎯 Next Resumption Focus: Restore the production relay (ISSUE-NOV5.0-O1)

1. Check the Render service for `gps-relay-server`; it timed out over HTTPS on Nov 5 between about 10:46 and 11:00.
2. Re-run the two-device test against the production relay, including Chapter 118.8 (relay hot switch), which has automated coverage only.
3. Decide whether the 100ms telemetry interval while charging is intended (ISSUE-NOV5.0-O3).
4. Schedule the manual physical chapters and the stress protocol (ISSUE-NOV5.0-O4).

### 🛡️ Implementation Status (Nov5.0)
- **Version Increment**: ✅ **VERIFIED** (`Nov5.0`, `versionCode` 31 monitor, 33 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (100/100, Run 5; instrumented tests pinned to A15).
- **Two-Device Live Run**: ✅ **PASSED through a local relay** (Sensor A15, Monitor S21FE).
- **Production Relay Path**: ❌ **NOT TESTED** (relay unresponsive).
- **Manual Physical Chapters & Stress Protocol**: ❌ **NOT EXECUTED** (see `TESTS/test_results.md`).

---

## 📂 Key Implementation Files
- `core-ui/.../HudStatusBar.kt`: `compact` mode and left-to-right enforcement.
- `app-monitor/.../MapCenterResolver.kt` (new): `resolveMapCenter`, `knownPositions`, `MapPosition`.
- `app-monitor/.../MonitorLocationSource.kt` (new): the Monitor's own position stream.
- `app-monitor/.../MonitorActivity.kt`, `MonitorViewModel.kt`, `MapModels.kt`, `MapComponents.kt`, `AndroidManifest.xml`: permission flow and map wiring.
- `core-network/.../SocketioSignalingClient.kt`: owned telemetry copies, payload fields, `viewer_status_relay`, relay switching.
- `core-shared/.../Models.kt`: `LocationUpdate.snapshot()`.
- Tests: `MapCenterResolverTest`, `HudLayoutAuditTest`, `SignalingBatchTest`, `BackfillContentionTest`, `SignalingClientTest`.

---

## ⚠️ Operational Notes
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- The relay persists `device_cache.json` and replays it to clients, so do not send simulated positions to the production relay.
- After a test run the Sensor app needs its service started again from `START SENSOR SERVICE`.

---

## ✅ Verified Milestones (Nov5.0)
1. **Automated Suite**: 100/100 on the final code.
2. **Hardware**: Live A15 to S21FE telemetry, with zero Davey or StackLog from either app process.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov5.0/`; tag `Nov5.0` is applied by the Git block above.

**Handover complete. System state locked at Nov5.0.**

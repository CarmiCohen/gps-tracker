# Resolution Archive

## ✅ Resolved Issues (Nov7.9)
- **HUD Flow 2Hz Rate Limiting (ISSUE-NOV6.0-O21)**: Applied `atUiRate()` (500ms sampling window) to `localHudState` and `remoteHudState` in `MonitorViewModel.kt` and to `hudState` in `SensorViewModel.kt`. Throttled incoming telemetry/fix inputs (`uiTelemetry = pairedTelemetry.atUiRate()`, `uiFix = fixStore.fix.atUiRate()`) before combining in `localHudState`, `remoteHudState`, and `hudState`, preventing 10Hz re-evaluations of expensive system service checks (`isNetworkAvailable`, `isLocationProviderEnabled`) and capping UI emissions to 2Hz. Verified by `UiRateTest` and new `HudRateTest` unit tests and full automated test suite pass across all modules. **PASSED.**

## ✅ Resolved Issues (Nov7.7)
- **False MOVING & Sensor-Detected Violations (ISSUE-NOV5.2-O13, group 1 slice 3)**: Fixed false MOVING on the Sensor by wiring `SentinelLogic.validateMovement` with accelerometer motion (`hasPhysicalMotion`) on each fix in `SensorService.kt`, transmitting motion state and violation status (`JUMP`, `OUTLIER`) in telemetry and via `ViolationEvent`. Added `AlarmType.GPS_JUMP` to `MonitorAlarms.kt`, wired to `AlertSettings.jammerDetection` and `signalingClient.violationFlow` in `MonitorAlarmEngine.kt`. Verified by unit tests in `MonitorAlarmsTest` and full automated test suite pass across all subprojects. **PASSED.**

## ✅ Resolved Issues (Nov7.6)
- **30-Second Siren Auto-Stop (ISSUE-NOV7.6-O40)**: Implemented 30-second timer auto-stop for Monitor alarms in `MonitorAlarmEngine.kt`: the siren automatically stops after 30 seconds (`30s`) or when acknowledged by the user, whichever occurs first. When the siren auto-stops via the 30-second timer, the visual alarm and notification remain pending until explicitly acknowledged. Verified by automated tests and test suite pass across all modules. **PASSED.**

## ✅ Resolved Issues (Nov7.5)
- **Communication Alarms (ISSUE-NOV5.2-O11, group 1, slice 2)**: Added communication alarms to `MonitorAlarms.kt` (`AlarmType.RELAY_DISCONNECTED`, `LOCAL_INTERNET_OFFLINE`, `GPS_SIGNAL_LOSS`), switched by `relayConnection`, `localInternet`, and `signalLoss` in `AlertSettings`. Integrated into `alarmConditions` and `alarmBooks`, wired to `signalingClient.isConnected` and `localInternetFlow()` in `MonitorAlarmEngine.kt`. Verified by 18 unit tests in `MonitorAlarmsTest` and full automated test suite pass across all modules. **PASSED.**

# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov8.0)

## 🎯 Next Resumption Focus: Work Queue after Nov8.0, solved one by one in the order below

### Work Queue
Nov8.0 is closed and followed Nov7.38, Nov7.36, Nov7.34, Nov7.32, Nov7.30, Nov7.28, Nov7.26, Nov7.24, Nov7.21, Nov7.19, Nov7.17, Nov7.15, Nov7.12, Nov7.11, Nov7.10, Nov7.9, Nov7.8, Nov7.7, Nov7.6, Nov7.5, Nov7.3, Nov7.2, Nov7.1, Nov7.0, Nov6.4, Nov6.3, Nov6.2, Nov6.1, Nov6.0, and Nov5.2; there was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, Nov7.16, Nov7.18, Nov7.20, Nov7.22, Nov7.23, Nov7.25, or Nov7.27 release. Issue IDs that contain NOV5.4 keep their names. Take one item at a time, and display it before fixing it (Rule 1).

**Next item: ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).**

| # | Issue | Work | Needs |
| :--- | :--- | :--- | :--- |
| 1 | ISSUE-NOV5.4-O17 | **Done for Nov6.0**: A chapters reviewed, chapter 87 decided, QA recounted at 41 of 110 | Nothing |
| 2 | ISSUE-NOV5.4-O18 | **Done for Nov6.0**: chapter retirement released | Nothing |
| 3 | ISSUE-NOV5.0-O2 | **Done for Nov6.1**: both Monitor HUD lines show only real values | Nothing |
| 4 | ISSUE-NOV5.2-O14 | **Done for Nov6.2**: Monitor trail storage bounded per tracker | Nothing |
| 5 | ISSUE-NOV5.0-O5 | **Done for Nov6.3**: the Setup dialog shows the real relay state | Nothing |
| 6 | ISSUE-NOV5.0-O6 | **Done for Nov7.0**: versions come from the installed package, and no production string holds one | Nothing |
| 7 | ISSUE-NOV5.2-O15 | **Done for Nov7.1**: every Monitor control that unpairs asks first | Nothing |
| 8 | ISSUE-NOV5.2-O11 | **Decided**: Group 1 (Alarms), Group 3 (`SentinelLogic`), Group 2 (Geofencing), Group 4 (Microphone), and Group 5 (Lift) to be built. Group 6 (Remote Siren), Group 7 (Hardening), and Group 8 (Export/Tools) retired/not required. Group 7 Extra Sensors TBD. | Action items pending implementation |
| 9 | ISSUE-NOV5.2-O13 | **Done for Nov7.7**: Sensor runs `SentinelLogic.validateMovement` with accelerometer motion on each fix and sends motion state / violation status in telemetry and via `ViolationEvent`; Monitor sounds GPS jump alarm | Nothing |
| 10 | ISSUE-NOV5.2-O12 | **Done for Nov6.4**: backfill round trip seen on the production relay | Nothing |
| 11 | ISSUE-NOV5.4-O19 | **Decided 2026-11-07**: `before splitting/gps-tracker` is a frozen archive; leave its changes as they are and never edit it. Only its redundant `relay-server/index.js` edit is to be discarded, by the user | User runs `git restore index.js` in `before splitting/gps-tracker/relay-server` |
| 12 | ISSUE-NOV6.0-O20 | **Done for Nov6.0**: forensic entries protected against interrupted writes and power loss | Nothing |
| 13 | ISSUE-NOV6.0-O23 | **Partly done for Nov7.8**: `IdentityPersistenceTest` reopen test added. Still open (found in Nov7.15, ISSUE-NOV7.15-O40): real tests for chapters 67, 68, 83, and 93, the stale wording listed in Open Gaps, and R494 and R495 in `VERIFICATION_MANIFEST.md` | Nothing |
| 14 | ISSUE-NOV6.0-O21 | **Done for Nov7.9**: HUD flows rate-limited to 2Hz via `atUiRate()` on `uiTelemetry`, `uiFix`, `localHudState`, `remoteHudState`, and `hudState` | Nothing |
| 15 | ISSUE-NOV6.0-O22 | **Done for Nov7.10**: overlay flags, dialog states, and follow mode updated to rememberSaveable in MonitorActivity and AdminActivity, persisting across rotation and configuration changes | Nothing |
| 16 | ISSUE-NOV6.0-O24 | **Done for Nov7.11**: SignalingQueue made thread safe via ReentrantLock and failed flush re-queuing preserves exact order at queue head via pushFront | Nothing |
| 17 | ISSUE-NOV6.0-O25 | **Done for Nov6.0**: calls missing on older Android removed, together with O20 | Nothing |
| 18 | ISSUE-NOV6.0-O26 | Run the Sensor on a device or emulator below API 35 | A device, or user permission to download an API 34 emulator image |
| 19 | ISSUE-NOV6.1-O27 | **Done for Nov7.2**: the Sensor HUD shows the service's real fix, or `--` | Nothing |
| 20 | ISSUE-NOV6.1-O28 | **Done for Nov7.12**: MonitorLocationSource filters fixes with isBetterLocation, preventing coarse network fixes from overriding fresh GPS fixes | Nothing |
| 21 | ISSUE-NOV6.2-O29 | **Done for Nov7.38**: Monitor stores and displays its own movement path (`isLocalUser = true`) alongside remote tracker, activating `TrailDao.pruneLocalTrail` (chapter 148) | Nothing |
| 22 | ISSUE-NOV6.2-O30 | **Done for Nov8.0**: Retain historical data of unpaired trackers (do not auto-delete on unpairing, preserving history for potential re-pairing) (chapter 149) | Nothing |
| 23 | ISSUE-NOV6.2-O31 | **Done for Nov6.4**: unanswered backfill requests are released after 30 seconds | Nothing |
| 24 | ISSUE-NOV6.3-O32 | **Done for Nov6.4**: gaps are read from the stored trail, so no missing range is forgotten | Nothing |
| 25 | ISSUE-NOV6.4-O33 | **Done for Nov7.15**: the Sensor stops at the first backfill chunk it cannot send and never sends the completion chunk after it, so the Monitor asks again (`answerBackfill`, chapter 137) | Nothing |
| 26 | ISSUE-NOV6.4-O34 | **Done for Nov7.17**: the Sensor Setup dialog shows the service state reported by `SensorService` (`SensorServiceStatus`) and the real relay state; its buttons no longer squeeze `Close` (chapter 138) | Nothing |
| 27 | ISSUE-NOV7.1-O35 | **Done for Nov7.36**: CleanSetupOverlay actions clear geofence alert settings, reset operational statistics & trail DB, and trigger full system re-initialization independently (chapter 147) | Nothing |
| 28 | ISSUE-NOV7.2-O36 | **Done for Nov7.34**: AdminActivity auto-starts SensorService on launch, displays live status & uptime timer (HH:mm:ss), and provides protected STOP control requiring confirmation dialog verification before issuing ACTION_STOP_SERVICE (chapter 146) | Nothing |
| 29 | ISSUE-NOV7.2-O37 | **Done for Nov7.30**: unknown speed (-1f), battery (-1), and temperature (-1f) sent as unknown, displaying `--` on HUDs and guarding alarms (chapter 144) | Nothing |
| 30 | ISSUE-NOV7.3-O38 | **Done for Nov7.28**: Sensor siren test button disabled/replaced on Sensor (stealth tracker) via AlarmSoundOverlay and SensorViewModel cleanup (chapter 143) | Nothing |
| 31 | ISSUE-NOV7.3-O39 | **Done for Nov7.32**: Phone Setup overlay provides dedicated guidance and direct action buttons for Do Not Disturb (Notification Policy Access) and Full-Screen Intent permissions, ensuring critical alarms reach users under DND and lock screens (chapter 145) | Nothing |
| 33 | ISSUE-NOV7.15-O41 | Find the main-thread work at Sensor start that delays the first `AdminActivity` frame past 500 ms | Nothing |
| 32 | ISSUE-NOV7.15-O40 | Fill the missing Nov7.7 to Nov7.12 dashboard entries and Nov7.6 to Nov7.9 Resolved Gaps from `RESOLUTION_ARCHIVE.md` and the baselines | Nothing |
| 34 | ISSUE-NOV7.17-O42 | **Done for Nov7.19**: no relay alarm before the first connection or 30 s, GPS loss only after 30 s without a position, Sensor movement checked once per fix and within the GPS error (chapter 139) | Nothing |
| 35 | ISSUE-NOV7.19-O43 | **Done for Nov7.21**: `LocationUpdate.isCritical` decides delivery (SIMP-NOV7.19-S1); the Sensor sends `NO_FIX` without a position once its newest fix is 15 s old, and does not judge movement on fixes coarser than 100 m (chapter 140) | Nothing |
| 36 | ISSUE-NOV7.19-O44 | **Done for Nov7.26**: `PermissionGuard` in `AdminActivity` re-checks permissions on resume via `Lifecycle.Event.ON_RESUME` (chapter 142) | Nothing |
| 37 | ISSUE-NOV7.21-O45 | **Done for Nov7.24**: `SensorService` seeds initial battery state from sticky intent prior to sensing loop start (chapter 141) | Nothing |

---

## 🟢 Resolved Gaps (Nov8.0)
*   **Unpaired Tracker Trail Retention (ISSUE-NOV6.2-O30)**: Verified and tested that unpairing a tracker (clearing target ID via `identityManager.clearTargetId()`) does not delete historical trail data stored in Room database (`TrailDao`), preserving historical telemetry and movement points for potential future re-pairing. Automated by `UnpairedTrackerTrailRetentionTest`. SOT rule 1.385, test chapter 149. **PASSED.**

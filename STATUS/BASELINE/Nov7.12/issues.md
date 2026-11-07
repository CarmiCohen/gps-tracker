# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov7.12)

## 🎯 Next Resumption Focus: Work Queue after Nov7.12, solved one by one in the order below

### Work Queue
Nov7.12 is closed and followed Nov7.11, Nov7.10, Nov7.9, Nov7.8, Nov7.7, Nov7.6, Nov7.5, Nov7.3, Nov7.2, Nov7.1, Nov7.0, Nov6.4, Nov6.3, Nov6.2, Nov6.1, Nov6.0, and Nov5.2; there was no Nov5.3, Nov5.4, or Nov7.4 release. Issue IDs that contain NOV5.4 keep their names. Take one item at a time, and display it before fixing it (Rule 1).

**Next item: ISSUE-NOV6.0-O26 (item 18).** Run the Sensor on a device or emulator below API 35. If the user prefers other work, take the first item marked `Nothing`: ISSUE-NOV6.4-O33.

| # | Issue | Work | Needs |
| :--- | :--- | :--- | :--- |
| 1 | ISSUE-NOV5.4-O17 | **Done for Nov6.0**: A chapters reviewed, chapter 87 decided, QA recounted at 41 of 110 | Nothing |
| 2 | ISSUE-NOV5.4-O18 | **Done for Nov6.0**: chapter retirement released | Nothing |
| 3 | ISSUE-NOV5.0-O2 | **Done for Nov6.1**: both Monitor HUD lines show only real values | Nothing |
| 4 | ISSUE-NOV5.2-O14 | **Done for Nov6.2**: Monitor trail storage bounded per tracker | Nothing |
| 5 | ISSUE-NOV5.0-O5 | **Done for Nov6.3**: the Setup dialog shows the real relay state | Nothing |
| 6 | ISSUE-NOV5.0-O6 | **Done for Nov7.0**: versions come from the installed package, and no production string holds one | Nothing |
| 7 | ISSUE-NOV5.2-O15 | **Done for Nov7.1**: every Monitor control that unpairs asks first | Nothing |
| 8 | ISSUE-NOV5.2-O11 | Group 1 alarms: **slice 1 done for Nov7.3** (Monitor-side alarms), **slice 2 done for Nov7.5** (communication alarms), **slice 3 done for Nov7.7** (Sensor-detected violations, GPS jump alarm). Group 3 (`SentinelLogic`): **built on the Sensor** (Nov7.7). Groups 2 and 4 to 8: decide build or retire | Groups 2 and 4 to 8: user decisions |
| 9 | ISSUE-NOV5.2-O13 | **Done for Nov7.7**: Sensor runs `SentinelLogic.validateMovement` with accelerometer motion on each fix and sends motion state / violation status in telemetry and via `ViolationEvent`; Monitor sounds GPS jump alarm | Nothing |
| 10 | ISSUE-NOV5.2-O12 | **Done for Nov6.4**: backfill round trip seen on the production relay | Nothing |
| 11 | ISSUE-NOV5.4-O19 | **Decided 2026-11-07**: `before splitting/gps-tracker` is a frozen archive; leave its changes as they are and never edit it. Only its redundant `relay-server/index.js` edit is to be discarded, by the user | User runs `git restore index.js` in `before splitting/gps-tracker/relay-server` |
| 12 | ISSUE-NOV6.0-O20 | **Done for Nov6.0**: forensic entries protected against interrupted writes and power loss | Nothing |
| 13 | ISSUE-NOV6.0-O23 | **Done for Nov7.8**: test hardening & chapter alignment (IdentityPersistenceTest reopen test added) | Nothing |
| 14 | ISSUE-NOV6.0-O21 | **Done for Nov7.9**: HUD flows rate-limited to 2Hz via `atUiRate()` on `uiTelemetry`, `uiFix`, `localHudState`, `remoteHudState`, and `hudState` | Nothing |
| 15 | ISSUE-NOV6.0-O22 | **Done for Nov7.10**: overlay flags, dialog states, and follow mode updated to rememberSaveable in MonitorActivity and AdminActivity, persisting across rotation and configuration changes | Nothing |
| 16 | ISSUE-NOV6.0-O24 | **Done for Nov7.11**: SignalingQueue made thread safe via ReentrantLock and failed flush re-queuing preserves exact order at queue head via pushFront | Nothing |
| 17 | ISSUE-NOV6.0-O25 | **Done for Nov6.0**: calls missing on older Android removed, together with O20 | Nothing |
| 18 | ISSUE-NOV6.0-O26 | Run the Sensor on a device or emulator below API 35 | A device, or user permission to download an API 34 emulator image |
| 19 | ISSUE-NOV6.1-O27 | **Done for Nov7.2**: the Sensor HUD shows the service's real fix, or `--` | Nothing |
| 20 | ISSUE-NOV6.1-O28 | **Done for Nov7.12**: MonitorLocationSource filters fixes with isBetterLocation, preventing coarse network fixes from overriding fresh GPS fixes | Nothing |
| 21 | ISSUE-NOV6.2-O29 | Decide whether the Monitor stores its own trail, with item 8 | User decision |
| 22 | ISSUE-NOV6.2-O30 | Decide what happens to the stored rows of a tracker that is no longer paired | User decision |
| 23 | ISSUE-NOV6.2-O31 | **Done for Nov6.4**: unanswered backfill requests are released after 30 seconds | Nothing |
| 24 | ISSUE-NOV6.3-O32 | **Done for Nov6.4**: gaps are read from the stored trail, so no missing range is forgotten | Nothing |
| 25 | ISSUE-NOV6.4-O33 | Stop the Sensor sending later backfill chunks after one fails | Nothing |
| 26 | ISSUE-NOV6.4-O34 | Replace the fixed status text in the Sensor Setup dialog | Nothing |
| 27 | ISSUE-NOV7.1-O35 | Decide what the three Monitor `CLEAN` actions should do, with item 8 | User decision |
| 28 | ISSUE-NOV7.2-O36 | Decide whether the Sensor service can be stopped on the device, with item 8 | User decision |
| 29 | ISSUE-NOV7.2-O37 | Send unknown speed, battery, and temperature as unknown, not as invented numbers | Nothing |
| 30 | ISSUE-NOV7.3-O38 | Remove or relabel the Sensor's siren test button, which can never sound | Nothing |
| 31 | ISSUE-NOV7.3-O39 | Let the alarm reach the user under Do Not Disturb and over a locked screen | User decision on the permission flow |

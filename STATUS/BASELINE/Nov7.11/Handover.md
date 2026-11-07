git add .
git commit -m "Nov7.11 - THREAD-SAFE SIGNALING QUEUE & ORDER-PRESERVING FAILED FLUSH (ISSUE-NOV6.0-O24)

- ISSUE-NOV6.0-O24: Made SignalingQueue thread-safe by protecting all queue manipulations and sequence tracking under ReentrantLock. Updated SocketioSignalingClient to push failed telemetry batches back to the front of the queue in their exact original sequence order via pushFront, preventing reordering behind newer incoming updates.
- TESTS: Verified all 227 automated unit tests passing across all subprojects (:core-shared, :core-network, :core-ui, :app-monitor, :app-sensor).
- DOCS: Updated issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md, VERIFICATION_MANIFEST.md, TESTS/TEST_PROCEDURE.md (Chapter 135 added), CHAPTER_TRIAGE.md (Chapter 135 added), and created baseline STATUS/BASELINE/Nov7.11/.
- VERSION: Nov7.11, versionCode 46 (monitor) and 48 (sensor)."
git tag Nov7.11
git push origin main --tags

# Project Handover - Version Nov7.11 (Thread-Safe Signaling Queue & Order-Preserving Failed Flush)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.11 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.11 commit and tag exist. Previous commits: `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O26 (item 18): run the Sensor on a device or emulator below API 35. If the user prefers other work, take the first item marked `Nothing`: ISSUE-NOV6.1-O28 (stop a coarse network fix replacing a better GPS fix). Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms) shipped in Nov7.5; 30-second siren auto-stop shipped in Nov7.6; slice 3 (Sensor-detected violations, GPS jump alarm, false MOVING fix with `SentinelLogic.validateMovement`) shipped in Nov7.7. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.11.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O28 Coarse network fix replacing GPS fix | Low | `MonitorLocationSource.kt` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.11)

Version Nov7.11 delivers ISSUE-NOV6.0-O24 (Thread-Safe Signaling Queue & Order-Preserving Failed Flush):
1. **Thread Safety & Order-Preserving Failed Flush**: Protected all queue operations in `SignalingQueue` with `ReentrantLock` for thread-safe concurrent access between offering and polling/flushing. Updated `SocketioSignalingClient` to push failed telemetry batches back to the front of the queue in their exact original sequence order via `pushFront`, preventing reordering behind newer incoming updates.
2. **Automated Suite**: All 227 automated unit tests passing across all subprojects (`:core-shared`, `:core-network`, `:core-ui`, `:app-monitor`, `:app-sensor`).
3. **Documentation & Manifest Update**: Updated `TESTS/CHAPTER_TRIAGE.md` (Chapter 135 added), `TESTS/TEST_PROCEDURE.md` (Chapter 135 added), `STATUS/SOT_MASTER_REQUIREMENTS.md` (Rule 1.371 added), `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `VERIFICATION_MANIFEST.md`, and created baseline `STATUS/BASELINE/Nov7.11/`.

**Resumption Token**: NOV7.11-QUEUE-O26-NEXT

---

## 📂 Key Implementation Files
- `core-network/.../SignalingQueue.kt`, `core-network/.../SocketioSignalingClient.kt`, `core-network/src/test/.../SignalingQueueTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.11.**

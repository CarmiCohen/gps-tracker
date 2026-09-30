# Issue #1391: Alarm Leakage on Tracker (Stealth Violation)

## 🎯 Status: Open
**Category**: Security / Operational Stealth

---

## 📝 Description
Alarms and sirens are manifesting locally on the device in **Tracker Mode**, violating the stealth requirement **R872**. Audible sirens or Red Screen overlays should only ever manifest on the **Viewer** device.

## 🛠️ Root Cause Analysis (Preliminary)
- Recent integration of **Reactive Red-Screen Promotion (Issue #1389)** and **Siren UI Synchronization (Issue #1382)** likely bypassed the role-based suppression logic.
- The `MainViewModel` may be promoting the `AlarmOverlay` based on `activeAlarmsFlow` without verifying if the local device is in `Tracker` mode.

## 🎯 Requirements & Goals
- **R872 Compliance**: ABSOLUTE silence and visual darkness for alarms on Trackers.
- **Audio Suppression**: Re-verify `AudioSynthesizer` blocks all local siren playback in Tracker mode.
- **Visual Suppression**: Ensure `MainViewModel` and `AlarmOverlay` are inhibited when `isTrackerMode` is true.

## 🔗 References
- **Source of Truth**: R872 (Tracker Stealth Authority)
- **Historical Anchor**: Issue #872

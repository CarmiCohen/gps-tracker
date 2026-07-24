# Issue #502: Device Independency (Hardware Heuristic Recovery)

## 🎯 Status: Resolved (Historical)
**Category**: Hardware Compatibility / Reliability

---

## 📝 Description
Budget devices (specifically Samsung A-series) and those with aggressive battery management were intermittently terminating sensor listeners or dropping relay connections without triggering standard OS callbacks.

## 🛠️ Resolution
- Implemented **Heuristic Recovery**: The system now monitors the "Logic Heartbeat". If a gap > 15s is detected, it proactively re-initializes hardware listeners.
- Added `openHardwareSettings` in `Utils.kt` to encapsulate vendor-specific (Xiaomi, Samsung) background settings.
- Established a mandatory 60s cooldown for hardware revival to prevent power-save oscillation.

## 🔗 References
- **Requirement**: R405c (Samsung Stay-Alive Hardening)
- **File**: `app/src/main/java/com/gps19/app/TrackerService.kt`

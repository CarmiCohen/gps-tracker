# Issue #113: Foreground Service & Budget Device Hardening

## 🎯 Status: Resolved (July.24.02)
**Category**: Service Reliability / Samsung A15

---

## 📝 Description
Budget hardware (Samsung A15) was aggressively terminating background services. Additionally, the transition from landing page to monitoring mode was causing race conditions in `startForeground`.

## 🛠️ Resolution
- Implemented **Double-Throttling** for FGS updates: 2s gate in `AppNotificationManager` and 10s global throttle for service type changes.
- Added **Hardware Poke** logic: Proactively refreshes WakeLocks and sensor registrations every 10s on A15 devices.
- Hardened **Automatic Restoration**: Services now verify all critical permissions before attempting to claim foreground status.
- Implemented **Startup Silence**: Suppresses non-essential status pulses until the system is marked "Active".

## 🔗 References
- **Requirements**: R993b, R993c, R107b, R405c
- **Files**: `ViewerService.kt`, `TrackerService.kt`, `AppNotificationManager.kt`

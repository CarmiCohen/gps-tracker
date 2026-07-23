# Issue #113: Samsung A15 Fallback Hardening (R405c)

## Status: Resolved (Implementation) / Pending Field Validation
## Requirement: R405c

### Description
Samsung A15 devices (budget hardware) exhibit aggressive OS-level background eviction even when a Foreground Service is active. The "Stay-Alive Pulse" was failing to prevent deep sleep.

### Resolution
- **Hardware Poke**: Upgraded the `SystemMonitor` to trigger a hardware "poke" via `WakeLock` and a minor Accelerometer sensor request every 10 seconds.
- **Service Promotion**: Forced `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` for the monitor lifecycle on this specific hardware profile.

### Verification (Field Test Pending)
- [ ] Long-term (4h+) field test on A15 hardware.
- [ ] Confirm no service eviction occurs during extended stationary periods.

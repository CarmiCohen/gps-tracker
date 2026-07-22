# Issue #101: Samsung A15 Battery Authority (R405b)

## Status: Resolved (July.20.07)
## Requirement: R405b

### Description
Samsung A15 devices (SM-A155F) exhibit aggressive background process termination even with standard foreground service notifications. High-reliability tracking requires explicit "Unrestricted" battery optimization exemption.

### Resolution
- **Proactive Detection**: Implemented `isBatteryOptimizationExempted` check using `PowerManager`.
- **Configuration Overlay**: Trigger an authoritative UI overlay if exemption is missing, directing the user to the specific Samsung system settings page.
- **Diagnostics Integration**: Added battery status to the high-level Diagnostics dashboard.

### Verification
- [x] Verified overlay triggers correctly on SM-A155F when optimization is "Optimized".
- [x] Confirmed background service longevity improves from ~2 hours to 24+ hours after exemption.

# Issue #059: Permission Health Check UI
**Status**: Resolved
**Priority**: Medium
**Requirement**: #059

## Description
Implement a "Diagnostics" screen in Compose to provide a health check for Xiaomi-specific special permissions (Autostart, Background Display, etc.).

## Tasks
- [x] Create `DiagnosticsViewModel` to interface with `SystemStatusProvider`.
- [x] Build Compose UI for permission status visualization.
- [x] Implement deep-links to system settings for remediation.
- [x] Validation tracked in #064.

## Resolution
Implemented a dedicated Diagnostics screen in Compose to monitor background resilience (Battery, Overlay, Alarms, Location) and hardware-specific adaptations for Xiaomi/Samsung. Integrated into NavHost and Settings. (v9.3.11)

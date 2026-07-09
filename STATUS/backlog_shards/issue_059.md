# Issue #059: Permission Health Check UI
**Status**: Open
**Priority**: Medium
**Requirement**: #059

## Description
Implement a "Diagnostics" screen in Compose to provide a health check for Xiaomi-specific special permissions (Autostart, Background Display, etc.).

## Tasks
- Create `DiagnosticsViewModel` to interface with `SystemStatusProvider`.
- Build Compose UI for permission status visualization.
- Implement deep-links to system settings for remediation.
- Validation tracked in #064.

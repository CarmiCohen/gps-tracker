# Project Handover: Samsung A15 Hardening & v9.3.20 Release Prep

## 📌 Status Summary
This document provides a definitive snapshot of the project as of v9.3.20-dev. The focus of this cycle is the simplification of device-specific logic for the Samsung A15 (R405).

### 1. Architectural Baseline (v9.3.20)
- **Current Version**: v9.3.20-dev
- **R405 Hardening Model**:
    - **Unified Heartbeat**: `TICK_INTERVAL_MS` standardized to **2000L (2s)** for all devices.
    - **Power Authority**: Proactive `IGNORE_BATTERY_OPTIMIZATIONS` check implemented in `MainActivity` for A15 devices.
    - **Manifest Authority**: Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission.

### 2. Active Development (v9.3.20)
- [ ] Implement low-power sensor subscription (`Step 5`) to replace remaining engine-level A15 switch-cases.
- [ ] Verify background stability on A15 with unified 2s heartbeat.

### 3. Recently Resolved (v9.3.18)
- **R404 (Legacy Relay URL Fallback)**: Resolved centralized config authority.
- **R403 (Startup ANR / Skipped Frames)**: Resolved via dynamic heartbeat recovery logic (since superseded by R405 unification).

### 4. Roadmap for v9.3.20
1. **Engine Simplification**: Remove `isA15` flags from `LocationProcessor` and `LocationSentinel`.
2. **Sensor Hardening**: Implement the "Stay-Alive" sensor listener in `AppSensorManager`.

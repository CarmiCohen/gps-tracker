# Walkthrough - Sensor & Monitor Deployment

## Overview
Successfully built, deployed, and tested both the **Sensor App** (`:app-sensor`) on the **Galaxy A15** (`R58X40GV2AR`) and the **Monitor App** (`:app-monitor`) on the **Galaxy S21 FE** (`R5CRC14PG4F`).

## Changes & Deployments
- **Sensor App (`:app-sensor`)**:
  - Built and installed `app-sensor-debug.apk` onto Galaxy A15.
  - Verified initialization of `AdminActivity`, `SensorService`, FGS permissions, and location/sensor event tracking.
- **Monitor App (`:app-monitor`)**:
  - Built and installed `app-monitor-debug.apk` onto Galaxy S21 FE.
  - Verified `MonitorActivity`, pairing wizard UI, and osmdroid map integration.
- **Identity & Pairing**:
  - Documented forensic 64-character SHA-256 identity generation via `ForensicIdentityManager`.
- **Issues Tracking**:
  - Logged non-blocking `SnapshotStateList` lock verification warning in [issues.md](file:///C:/CCwork/JD/issues.md).

## Verification Results
- **Build**: PASSED (`assembleDebug` for both `:app-sensor` and `:app-monitor`).
- **Deployment**: PASSED on both physical devices.
- **Live Event Logs**: Verified sensor telemetry generation and foreground state transitions.

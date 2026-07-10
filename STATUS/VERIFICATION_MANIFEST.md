# Compliance & Operational Requirements (Audit Baseline) - v9.3.11

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions, see [RESOLUTION_ARCHIVE.md](RESOLUTION_ARCHIVE.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R997** | **Background Resilience Health Check**: Mandatory Diagnostics interface for hardware-specific hardening. | **Verified (v9.3.11)** |
| **R996 / #005** | **Log Spillage Hardening**: Silence logcat spam regarding `getPackageName` via cached identifiers. | **Verified (v9.3.11)** |
| **R995** | **Signaling Pulse Acknowledgement**: Tracker MUST update remote activity timestamp upon pulse receipt. | **Verified (v9.3.10)** |
| **R981** | **Map Marker Stability Authority**: Markers MUST use `optimizedPoint` from `LocationProcessor`. | **Verified (v9.3.8)** |
| **R980** | **Peer Activity HUD Authority**: Role-specific freshness logic for peer badges. | **Verified (v9.3.8)** |
| **R978** | **Service Component Injection**: Core service components MUST be field-injected via Hilt. | **Verified (v9.3.6)** |
| **R973** | **Standardized Proto Path Authority**: `app/src/main/proto` is the sole schema directory. | **Verified (v9.3.0)** |
| **R400** | **Map Metadata Alignment**: Status messages anchored to bottom-center with 80dp offset. | **Verified (v9.3.0)** |
| **R994** | **Screen-Off Optimization Authority**: GPS polling frequency throttled when screen is off. | **Verified (v9.2.9)** |
| **R993** | **Notification Throttling Authority**: Foreground service notification update throttling. | **Verified (v9.2.8)** |
| **R990** | **Anchor Lock Breakout**: Displacement-weighted monitor for breakout (#053 / #062). | 🟡 **Pending Validation** |
| **R989** | **HUD Freshness Duality**: Differentiation between Telemetry and GPS freshness. | **Verified (v9.2.0)** |
| **R987** | **Speed Zeroing Authority**: Immediate speed drop to 0.0 on GPS loss. | **Verified (v9.3.6)** |
| **R986** | **State Sync Audit**: Simultaneous Tracker/Viewer HUD state transitions. | **Verified (v9.3.6)** |
| **R968** | **Proto Precision Integrity**: Preservation of max_distance/max_accuracy in UI. | 🟡 **Pending Validation** |
| **#031** | **Soak Test Monitoring**: 24-hour stability audit for stability gaps. | 🟡 **Pending Validation** |
| **#039** | **Identity Rejection UX**: UI feedback for ID collisions (R977). | **Verified (v9.3.4)** |
| **#042** | **Sanitization Visibility**: UI notification for auto-sanitized IDs (R976). | **Verified (v9.3.2)** |

## 2. Resolution Archive (Hardening Phase)

### 2.1. Hardening Phase Resolutions (v9.3.11)
*   **FIXED #059: Permission Health Check UI** - Resolution: Implemented Diagnostics screen in Compose for background resilience monitoring (Xiaomi/Samsung). (Requirement R997)
*   **FIXED #068: Logcat Audit (Samsung Spam)** - Resolution: Hardened `Utils.kt` to use cached package names, eliminating `getPackageName` noise. (Requirement R996)

### 2.2. Hardening Phase Resolutions (v9.3.10)
*   **FIXED #073: Peer Visibility (Issue C)** - Resolution: Added mandatory signaling pulse acknowledgement in `TrackerService`. (Requirement R995)

### 2.3. Hardening Phase Resolutions (v9.3.8)
*   **FIXED #074: Incorrect Peer Activity Badge Logic** - Resolution: Corrected `isPeerActive` logic in `GlobalStatusBar`. (Requirement R980)
*   **FIXED #072: Map Stabilization** - Resolution: Updated `RemoteHandler` to use `optimizedPoint` from `LocationProcessor`. (Requirement R981)

### 2.4. Hardening Phase Resolutions (v9.3.6)
*   **FIXED #058: TrackerService Initialization** - Resolution: Migrated all common service dependencies to `BaseMonitorService`. (Requirement R978)

**Full history available in [RESOLUTION_ARCHIVE.md](RESOLUTION_ARCHIVE.md).**

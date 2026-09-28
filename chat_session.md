# 📑 Forensic Chat Session Log: Issue #1205 (In Progress)

## 🏁 Session Metadata
*   **Version Name**: Sep.27.18
*   **Target Issue ID**: #1205
*   **Title**: Context-Aware Power Optimization
*   **Significance**: Medium (Battery)
*   **Status**: Models Extended, Discovery Complete, Logic Implementation Pending.

---

## 🔍 Comprehensive Architectural Discovery & Codebase Mapping

We have performed an exhaustive discovery phase across the `:app` and `:core:engine` subprojects to understand the constraints and integration points for implementing context-aware power management.

### 1. Centralized Polling Authorities (`EngineConstants.kt`)
The tracking engine utilizes central constants defined in `com.gps19.core.engine.EngineConstants.kt` to enforce device polling strategies:
*   `HIGH_FREQUENCY_GPS_POLLING_MS` = 2000L
*   `MOVING_GPS_POLLING_MS` = 5000L
*   `STATIONARY_GPS_POLLING_MS` = 60000L
*   `SCREEN_OFF_GPS_POLLING_MS` = 45000L
*   `SUSPICIOUS_GPS_POLLING_MS` = 10000L
*   `COOLING_GPS_POLLING_MS` = 30000L
*   `VIEWER_GPS_POLLING_MS` = 10000L
*   `ULTRA_LONG_STATIONARY_GPS_POLLING_MS` = 300000L (5 minutes)

### 2. Behavioral State Control (`ServiceBehaviorUseCase.kt`)
The service behavior is determined dynamically inside `ServiceBehaviorUseCase.calculateGpsInterval(...)`:
*   Tracks stationary duration using a realtime delta (`nowRt - lastMotionDetectedTs`).
*   Transitions the interval into `STATIONARY_GPS_POLLING_MS` or `ULTRA_LONG_STATIONARY_GPS_POLLING_MS` when the physical status remains motionless.
*   **Gap Identified**: The current logic relies solely on physical vibration indices (`isStationary`) and time durations. It lacks finer-grained context awareness from high-level user activities.

### 3. Background Pipeline and Flyweights (`MonitorService.kt` & `HardwareSuite.kt`)
*   `MonitorService.kt` runs the background collection loops orchestrated via `TickOrchestrator`.
*   Uses flyweight pooling (`evaluationSnapshotFlyweight`, etc.) to eliminate GC churn.
*   `HardwareSuite.kt` controls sensor registration and `fusedLocationClient`.
*   Permission state auditing already proactively checks for `ACTIVITY_RECOGNITION`.

---

## 🛠 Progress & Modifications

### 1. Core Engine Model Modifications (`EngineModels.kt` & `LocationUpdate.kt`)
*   **`ActivityType` Enum**: Defined a serializable enum representing discrete operational contexts: `STILL`, `WALKING`, `RUNNING`, `BICYCLING`, `IN_VEHICLE`, `TILTING`, `UNKNOWN`.
*   **`SystemEvaluationSnapshot`**: Added `activityType: ActivityType` field. Updated `copyFrom` and `reset` to support zero-allocation reuse.
*   **`EngineConnectionPoint`**: Added `activityType: ActivityType` field. Updated `copyFrom`.
*   **`EngineSensorSnapshot`**: Added `activityType: ActivityType` field. Updated `copyFrom`.
*   **`KineticState`**: Added `activityType: ActivityType` to the spatial telemetry container in `LocationUpdate.kt`.

### 2. State Synchronization
*   **`Handover.md`**: Updated with forensic state snapshot of the current architectural extension.

---

## 🔴 Remaining Tasks for Issue #1205
1.  **Activity Recognition Bridge**: Implement pattern-based or API-based activity classification in `HardwareSuite.kt`.
2.  **Adaptive Polling Scaling**: Update `ServiceBehaviorUseCase.calculateGpsInterval` to use `ActivityType` for faster or slower backoffs (e.g., immediate 1min polling if `STILL`).
3.  **Telemetry Mapping**: Update `TelemetryMapper.kt` to propagate `activityType` from snapshots to status updates and connection points.

---

**[Session Terminated by User Request]**

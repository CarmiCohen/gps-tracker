# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 277**

## 1. Architectural Synchronization & Service Hardening (v9.3.16)
*   **Issue #080**: Lift Detection Logic Inconsistency. Remediated `MainAlarmLogic` in `:core:engine` to compute violations using the relative barometer delta (Absolute - EMA) instead of raw absolute altitude. (Requirement R999b Parity)
*   **Issue #079**: TrackerService API Synchronization. Remediated compilation errors by aligning background service telemetry logic with current engine component signatures (`processGpsPoint`, `evaluateAlarms`, `pushCurrentStatus`). Implemented mandatory `getRequiredTickInterval()` for hardware-aware polling control. (Requirement R999b)
*   **Issue #078**: Map Centering Follow Conflict. Implemented `MapFollowMode` (TRACKER, VIEWER, AUTO) in `MainUiState`. Updated `OsmMap` lock logic to respect follow intent, preventing the map from snapping back to the tracker after user centers on the viewer.

## 2. Type Safety & Precision Hardening (v9.3.15)
*   **Issue #077**: Type Safety Hardening. Systematic audit and elimination of redundant `toDouble()`/`toFloat()` conversions across core engine and app modules. Implemented `Double` pre-buffering in `AppSensorManager`. (Requirement R999)

## 3. Background Resilience & Samsung Adaptations (v9.3.14)
*   **C-068-1**: Samsung System API Noise. Implemented 10s TTL caching for ALL permission and system status checks to eliminate Logcat noise on Samsung G990/A15. (Requirement R998)

## 4. Historical Resolutions (v9.3.1 - v9.3.13)
... [See historical logs for full 277 resolutions]

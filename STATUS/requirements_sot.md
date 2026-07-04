# System Source of Truth (SoT) - v8.9.90

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies. (Issue #322)
*   **Sensor Processing Authority (R965)**: `AppSensorManager` offloads all high-frequency sensor event processing to a dedicated `HandlerThread` (`AppSensorThread`). (Issue #006 / Issue #013 / v8.9.70)
*   **Connectivity Integrity (R966)**: `AppNetworkManager` implements a short-circuit reactive reconnection trigger. (Issue #007 / v8.9.64)
*   **Transport Authority**: The system strictly enforces `websocket` transport for low-latency signaling. (Issue #007 / v8.9.64)
*   **Service Launch Integrity (R926)**: The system enforces a mandatory **2,000ms delay** during session auto-transitions before launching background services. (Issue #320)
*   **Log Spillage Remediation (Issue #005)**: The system mandates a static User Agent and **manually defined storage paths** for third-party libraries (e.g., osmdroid) to prevent repetitive `getPackageName()` system logcat spam. (v8.9.90)
*   **Time Integrity**: All alarm evaluations and hardware latches use monotonic time via `TimeProvider.elapsedRealtime()`. (Issue #311 / Issue #441)
*   **OS Compatibility**: Authoritative baselines are **minSdk 24** and **targetSdk 35**. (v8.9.42)
*   **Reactive System State Flows (R945)**: Implementation of cold-to-hot reactive flows for system health (Battery, Internet, Permissions). (Issue #404)
*   **Watchdog Battery Optimization (R946)**: Conservative `AlarmManager` rescheduling utilizing exact alarms only when critical.
*   **Foreground Service Transition (R967)**: The system maintains a **45-second "Recent UI Pulse" window** (`UI_PULSE_TIMEOUT_MS`) to bridge Android 14+ `MICROPHONE` type transitions. (Issue #025 / v8.9.86)
*   **Type Safety Authority**: All telemetry fields (Accuracy, Speed, Bearing, Sensor Indices) are standardized to `Double` across the entire chain (Engine, App, Room). (Issue #014 / v8.9.75)
*   **Data Persistence Integrity (R968)**: All changes to Protobuf schemas must preserve binary compatibility. Migration from legacy types must use new field tags and explicit `DataMigration` logic. (Issue #023 / v8.9.84)

### 2. Branding & UI Standards
*   **Branding Authority (R865/R866)**: "Unified Identity Green" is strictly defined as **JD Branding Green (#367C2B)**.
*   **Icon Authority (R935)**: The authoritative application icon is the text-free John Deere deer logo.
*   **Role Identity Standards (R182)**: IDs are free-form strings. Prefixes "T" (Tracker) and "V" (Viewer) are mandated.
*   **VID Notes Authority (R924)**: **OBSOLETE (v8.9.89)**. The `VID_NOTES` string and its display in the `HeaderBar` have been removed.
*   *(Sections 3-5 omitted for brevity, see v8.9.86 for full location and hardware sentinel rules)*

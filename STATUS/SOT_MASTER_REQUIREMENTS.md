# SOT Master Requirements (Sep.02.68)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (41 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.17 Robust Battery Navigation (R896)**: **MANDATORY**. Battery optimization exemption intents MUST use `Uri.fromParts("package", pkg, null)` for URI encoding and implement a multi-tier fallback (Sep.02.40).
*   **1.18 Log Spillage Protection (R759)**: **MANDATORY**. All application-level logging MUST use `Timber`. Direct calls to `android.util.Log` or `System.out.println` are strictly prohibited (Sep.02.50).

## 🧩 Functional Requirements (207 IDs)
*   **R-ID 238 (Model Unification)**: The application MUST use `LocationUpdate` as the single source of truth for location data across both the Core Engine and UI layers (Sep.03.01).
*   **R-ID 240 (Tracker HUD Telemetry)**: `TrackerService` MUST publish telemetry to the repository every tick, regardless of GPS fix status, to ensure the local HUD remains live (Sep.03.15).
*   **R-ID 241 (Atomic Activation)**: The system MUST atomically persist `IS_SYSTEM_ACTIVE_KEY = true` during role selection to ensure that background workers and telemetry logic are unblocked before service initialization (Sep.02.66).
*   **R-ID 242 (Recovery Pipeline)**: The system MUST implement a reactive signal-response loop between the Activity lifecycle and the UI layer to automatically retry deferred foreground service starts upon `onResume` (Sep.02.66).
*   **R-ID 243 (Status Visibility)**: `GlobalStatusBar` MUST propagate the `isSystemActive` flag to all child indicators to provide unambiguous visual confirmation of tracking status (Sep.02.68).

*(Total: 41 Architectural Rules + 207 Functional R-IDs = 248 Items)*

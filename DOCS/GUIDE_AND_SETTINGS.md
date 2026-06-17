# Setup Guide & Configuration Mechanism (v8.8.35)

This document describes the implementation of the "Phone Setup" guide and the persistence logic for application settings.

## 1. The Setup Guide (`PhoneSetupOverlay`)
The `PhoneSetupOverlay` guides the user through the Android permissions and battery optimizations required for high-reliability background tracking.

### A. Core Sections
The guide targets specific system bottlenecks:
1.  **Lock in Recents**: Instructs the user to "lock" the app in the recent tasks list to prevent OS-level eviction.
2.  **Battery Mode**: Directs the user to enable "Unrestricted" battery mode.
3.  **Display Over Apps**: Essential for the `AlarmOverlay` to bypass the lock screen.
4.  **Microphone Access**: Essential for the Acoustic Sentinel.
5.  **Auto-start & Background Restrictions**: Specifically targets Xiaomi (MIUI/HyperOS), Samsung, and other aggressive OEMs. Verified via `isXiaomiAutostartGranted` (v8.8.35).
6.  **Wi-Fi Power Saving**: Advises the user to disable Wi-Fi sleep policies.
7.  **Exact Alarms**: Required for the watchdog system (`SYSTEM_WATCHDOG_INTERVAL_MS` 90s) on Android 12+.
8.  **Notification Permission**: Required for Foreground Service visibility.

### B. Device-Specific Logic
The guide dynamically adapts:
- **Xiaomi Detection**: Adds specialized sections for MIUI-specific permissions and the background "pop-up" window gate. Includes `ALERT_ID_XIAOMI_SYSTEM_MISSING` monitoring and manual override support. Guidance updated to "Phone Setup" (v8.8.35).
- **Samsung A15 Detection**: Adjusts proximity debounce thresholds (`PROXIMITY_DEBOUNCE_STATIONARY_A15_MS` 5s).

## 2. Setting Persistence (`MainViewModel` & `SettingsRepository`)
The application uses a **Reactive Draft System** for managing settings.

### A. Draft Logic
1.  A "Draft" copy of the settings is created in `MainViewModel`.
2.  UI interactions modify the **Draft** only.
3.  **Commit on Exit**: When the user exits the Settings overlay (via Back), the `CommitSettings` event writes the entire draft to the `DataStore` atomically.

### B. Role Agnostic Parity (R916)
Configuration fields (IDs, Geofence, Alerts, Sound) are accessible and modifiable in both Tracker and Viewer roles to ensure operational flexibility.

### C. Debounced Sync
Significant changes (Relay URL/IDs) trigger a debounced re-initialization of the network stack and service roles (`TrackerService` / `ViewerService`) to maintain the peer-to-peer link without thrashing.

## 3. Configuration Management
- **Load Config**: Import JSON configuration for instant setup.
- **Export Logs**: Provides a forensic snapshot of event history and system metrics. Standardized to include the mandatory `role` field (v8.8.35). Legacy version tags have been removed in favor of the simplified forensic model.

## 4. Navigation Refinement
- **Implicit Navigation**: Following the R920 rule, the buttons row relies on system Back navigation. The Settings page uses system gestures as the primary "Apply" trigger.
- **Sub-Navigation**: Nested overlays for Sound and Alert management minimize cognitive load.

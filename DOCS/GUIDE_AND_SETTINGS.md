# Setup Guide & Configuration Mechanism (v8.9.37)

This document describes the implementation of the "Phone Setup" guide and the persistence logic for application settings.

## 1. The Setup Guide (`PhoneSetupOverlay`)
The `PhoneSetupOverlay` guides the user through the Android permissions and battery optimizations required for high-reliability background tracking.

### A. Core Sections
The guide targets specific system bottlenecks:
1.  **Lock in Recents**: Prevents OS-level eviction.
2.  **Battery Mode**: Directs the user to enable "Unrestricted" battery mode.
3.  **Display Over Apps**: Essential for the `AlarmOverlay` to bypass the lock screen.
4.  **Microphone Access**: Essential for the Acoustic Sentinel.
5.  **Auto-start & Background Restrictions**: Specifically targets Xiaomi (MIUI/HyperOS) and Samsung. Verified via `isXiaomiAutostartGranted`.
6.  **Wi-Fi Power Saving**: Advises disabling Wi-Fi sleep policies.
7.  **Exact Alarms**: Required for the watchdog system on Android 12+.
8.  **Notification Permission**: Required for Foreground Service visibility.

### B. Device-Specific Logic
- **Xiaomi Detection**: Adds specialized sections for MIUI-specific permissions. Includes `ALERT_ID_XIAOMI_SYSTEM_MISSING` monitoring and manual override support. Guidance updated for v8.9.37 (Issue #190).
- **Samsung A15 Detection**: Adjusts proximity debounce thresholds (5s - Issue #191).

## 2. Setting Persistence (`MainViewModel` & `SettingsRepository`)
The application uses a **Reactive Draft System** for managing settings.

### A. Draft Logic
1.  A "Draft" copy of the settings is created in `MainViewModel`.
2.  UI interactions modify the **Draft** only.
3.  **Commit on Exit**: Exiting the Settings overlay triggers an atomic write to `DataStore` (Issue #413).

### B. Role Agnostic Parity
Configuration fields are accessible and modifiable in both Tracker and Viewer roles.

### C. Debounced Sync
Significant changes (Relay URL/IDs) trigger a debounced re-initialization of the network stack and service roles.

## 3. Configuration Management
- **Load Config**: Import JSON configuration for instant setup.
- **Export Logs**: Provides a forensic snapshot of event history. Standardized to include the mandatory `role` field and **Log Spatial Anchors** (Issue #208).

## 4. Navigation Refinement
- **Implicit Navigation**: The Settings page uses system Back gestures as the primary "Apply" trigger.
- **Ghost Mode UX**: Visual staleness indicators are applied globally (Issue #193).

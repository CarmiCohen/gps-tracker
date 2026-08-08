# Setup Guide & Configuration Mechanism (vAug.07.06)

This document describes the implementation of the "Phone Setup" guide and the persistence logic for application settings. As of vAug.07.06, all terminology follows the R747 Locality Authority.

## 1. The Setup Guide (`PhoneSetupOverlay`)
The `PhoneSetupOverlay` guides the user through the Android permissions and battery optimizations required for high-reliability background tracking.

### A. Core Sections
The guide targets specific system bottlenecks:
1.  **Lock in Recents**: Prevents OS-level eviction.
2.  **Battery Mode**: Directs the user to enable "Unrestricted" battery mode.
3.  **Display Over Apps**: Essential for the `AlarmOverlay` to bypass the lock screen.
4.  **Samsung A15 Battery Authority (R405b)**: Specialized guidance for Samsung A15 devices to prevent background service termination (Issue #101).
5.  **Auto-start & Background Restrictions**: Specifically targets Xiaomi (MIUI/HyperOS) and Samsung.

## 2. Setting Persistence (`SettingsRepository`)
The application uses a **Hilt-managed Jetpack DataStore** for configuration.

### A. Singleton Authority (R511)
To prevent `IllegalStateException` during startup, the `SettingsRepository` accesses DataStore via a `Context.dataStore` property delegate. This ensures exactly one instance exists per process, shared across all Hilt entry points (Issue #511).

### B. Reactive Updates
UI components observe settings via a `Flow<Settings>` exposed by the repository, ensuring real-time UI updates across Device and Viewer roles.

### C. Debounced Sync
Significant changes (Relay URL/IDs) trigger a debounced re-initialization of the network stack.

## 3. Configuration Management
- **Load Config**: Import JSON configuration for instant setup.
- **Export Logs**: Provides a forensic snapshot of event history. Standardized to include mandatory role fields and Log Locality (R747).

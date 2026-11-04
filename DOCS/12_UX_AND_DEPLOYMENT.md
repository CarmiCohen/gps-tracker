# Project Revolution: UX & Deployment Strategy

## 1. Elimination of the Landing Page
In the legacy monolithic app, the user had to select a role (Tracker vs. Viewer) upon startup. In the Revolution architecture, this is completely eliminated.
- **Distinct APKs**: The Sensor and Monitor are built as two separate Android applications (`:app-sensor` and `:app-monitor`).
- **Implicit Role**: The "Mode" is defined by which application is installed. There is no shared UI for role selection.
- **Startup Speed**: By removing the landing page, the Monitor boots directly into the Map view, and the Sensor boots directly into the Admin Console & Service, cutting down interaction time.

## 2. Deployment Profiles
- **Sensor Deployment**: Targeted at devices connected to a vehicle or permanent power source. The installation package is minimized for background stability.
- **Monitor Deployment**: Targeted at the user's primary mobile device. The installation package includes rich UI assets, map libraries, and control tools.

## 3. Configuration, Provisioning & UI Features
- **First-Run Sync & Pairing**: Upon first launch of the Monitor, `PairingWizard` prompts for the Target Tracker ID (default `"T"`).
- **Upper Menu Header**: Both applications feature a top menu header containing:
  - **Setup Button (`Build`)**: Opens the Setup & Info dialog displaying Device IDs, Version, Service Status, and active Relay URL.
  - **Settings Button (`Settings`)**: Opens configuration overlays for custom device IDs, Relay URL customization, and reset options.
  - **Disconnect / Close**: Quick action to stop services or re-pair devices.
- **Relay URL Configuration**: Users can customize the Relay Server URL (or reset to default `https://gps-relay-server.onrender.com`) in both Sensor and Monitor setup overlays, with automatic reactive signaling re-connection.
- **Map Control Wheel & Tools Overlay**: The Monitor app includes a map settings wheel (`MapSettingsToggle`) and controls for Zooming (`+`/`-`), Centering (`TRACKER`/`VIEWER`), Violation Toggles (`JUMP`/`OUT`), Trail I/O (`SAVE`/`LOAD`), Geofencing (`ADD`/`DEL`/`FENCE`), and Trail Clearing (`CLEAR`). See [MAP_FUNCTIONALITY_SPEC.md](MAP_FUNCTIONALITY_SPEC.md) for complete details.
- **Standardized HUD Status Bar**: Integrated 2-row `HudStatusBar` with compact 8.sp/9.sp status badges for real-time SRV, GPS, TRK/VWR, DAT, and WDG status reporting across both apps.

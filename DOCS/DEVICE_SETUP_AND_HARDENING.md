# Device-Specific Setup & Hardware Hardening Strategy (vNov4.31)

This document outlines the device-specific capabilities profiling and hardware hardening framework used in the GPS Tracker & Sentinel system to ensure high-assurance background telemetry continuity across diverse Android hardware (such as Samsung A15 budget tier and Samsung S21FE flagship tier).

## 1. Hardware Capabilities & Performance Profiling (`HardwareCapabilities`)
The system dynamically inspects device manufacturer and model characteristics on startup:
- **Budget / Staggered Tier (`PerformanceTier.STAGGERED`)**: Automatically assigned to budget devices such as Samsung A15 (`SM-A155F`), requiring staggered hardware pokes, aggressive wakelock policies, and reduced battery pressure.
- **Standard / High Tier (`PerformanceTier.STANDARD`)**: Assigned to standard and flagship devices (such as Samsung S21FE), utilizing standard polling intervals and default power management.
- **Vendor Flags**: Identifies Samsung, Huawei, and Xiaomi/Redmi devices to apply vendor-specific power management workarounds.

## 2. Device Hardening Strategy (`DeviceHardeningStrategy`)
Central authority managing vendor-specific background execution rules:
- **WakeLock Policy Enforcement**: Automatically applies vendor wakelock renewal rules for Samsung and Huawei devices to prevent radio and location sleep dropouts.
- **Background Transition Hardening (60s Window)**: Detects when the application transitions from foreground to background and activates a 60-second escalation window for staggered tier devices, eliminating telemetry jitter spikes.
- **Vendor Continuity Checks**: Enforces watchdog scheduling and listener priority preservation across vendor restrictions.

## 3. Device Profile Manager (`DeviceProfileManager`)
Coordinates loop continuity tweaks, hardware capabilities evaluation, and background thread scheduling.

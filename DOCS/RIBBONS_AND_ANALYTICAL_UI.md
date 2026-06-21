# Analytical Ribbons & Connection History (v8.9.10)

This document describes the high-density sparkline visualization system used for forensic telemetry analysis.

## 1. Ribbon Architecture
The "Ribbons" provide a time-series view of system health across six resolutions (4M to 7D). 
- **Data Source**: `HistoryEntity` in SQLite.
- **Aggregation**: Worst-case value selection per bucket.
- **Forensic Tagging**: Every point includes battery current (`currentMa`), speed, bearing, and SIT status.
- **Log Anchor (v8.9.10)**: Events occurring during ribbon windows are now geographically anchored, allowing users to tap a ribbon event and see its location on the map.

## 2. Ribbon Definitions
*   **SNR**: Signal-to-Noise Ratio (0-45dB). Indicates GPS environmental quality.
*   **NS**: Ambient Noise Floor (50-90dB). Used for acoustic tamper detection.
*   **LX**: Light levels (Lux). Detects enclosure tampering.
*   **VB**: Vibration magnitude. Distinguishes between engine idle and movement.
*   **PR**: Proximity status.
*   **LF**: Barometric Lift.
*   **CUR**: Battery Current (mA). Negative indicates drain; positive indicates charging.
*   **BAT**: Steep Discharge latch status.

## 3. UI Implementation
- **Rendering**: Custom Canvas drawing in `SharedUiComponents.kt`.
- **Ghost Mode**: Sparklines dim if the remote device is offline > 10s.
- **Version ID**: The build version (v8.9.10) is displayed to map screenshots to specific logic baselines.

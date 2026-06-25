# Analytical Ribbons & Connection History (v8.9.37)

This document describes the high-density sparkline visualization system used for forensic telemetry analysis.

## 1. Ribbon Architecture
The "Ribbons" provide a time-series view of system health across six resolutions (4M to 7D). 
- **Data Source**: `HistoryEntity` in SQLite.
- **Aggregation**: Worst-case value selection per bucket.
- **Forensic Tagging**: Every point includes battery current (`currentMa`), speed, bearing, and SIT status metrics (Issue #192).
- **Log Anchor**: Events occurring during ribbon windows are geographically anchored, allowing users to tap a ribbon event and see its location on the map (Issue #208).

## 2. Ribbon Definitions
*   **SNR**: Signal-to-Noise Ratio (0-45dB). Indicates GPS environmental quality (`RIBBON_SNR_SCALE_DB`).
*   **NS**: Ambient Noise Floor (0-40dB range). Used for acoustic tamper detection (`RIBBON_NOISE_SCALE_DB`).
*   **LX**: Light levels (Lux). Detects enclosure tampering (`RIBBON_LUX_LOG_SCALE`).
*   **VB**: Vibration magnitude. Distinguishes between engine idle and movement (`RIBBON_VIBRATION_SCALE_G`).
*   **TI**: Tilt Index. Derived from orientation stability (`RIBBON_SIT_TILT_SCALE_DEG`).
*   **BA**: Baro Index. Derived from altitude stability (`RIBBON_SIT_BARO_SCALE_METERS`).
*   **CUR**: Battery Current (mA). Negative indicates drain; positive indicates charging (`RIBBON_CURRENT_SCALE_MA`).
*   **BAT**: Steep Discharge latch status (Issue #221).

## 3. UI Implementation
- **Rendering**: Custom Canvas drawing in `SharedUiComponents.kt`.
- **Ghost Mode**: Sparklines dim (Slate500) if the remote device is offline > 10s (`TELEMETRY_UI_STALE_THRESHOLD_MS`) (Issue #193).
- **Version ID**: The build version (v8.9.37) is displayed to map screenshots to specific logic baselines.

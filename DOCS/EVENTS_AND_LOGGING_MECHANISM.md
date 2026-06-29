# Events and Logging Mechanism

This document describes how the system captures, processes, and displays forensic events.

## 🎨 Color Coding Standards (v8.9.48)
To ensure immediate situational awareness, the log and UI use a strict color-coding scheme aligned with the Branding Authority (R865/R866).

1.  **Critical / Error (Rose500 / Red)**: Triggered by `CRITICAL`, `ERROR`, `VIOLATION`, or active `SIREN` states. Indicates immediate action required.
2.  **Warning / Transition (Amber500 / Orange)**: Triggered by `TRACKER STATE` changes, signal degradation, or suspicious activity.
3.  **Success / Restored (BrandJd / JD Green)**: Triggered by `CONNECTED`, `RESTORED`, or general healthy heartbeats.
    *   *Note: Emerald500 is superseded by BrandJd (#367C2B) for all authoritative identity and success indicators.*
4.  **Viewer / User Action (ViewerOrange)**: Triggered by local user interactions or viewer-specific connectivity events.
5.  **Forensic Detail (Teal500 / Cyan)**: Used for SNR snapshots, coordinate data, and precise timestamps in detail panes.
6.  **Special Events (ForensicPink)**: Reserved for unique sensor triggers like `SITTING`, `VIBRATION`, or `TAMPER`.

## 📝 Logging Levels
- **Normal**: Standard telemetry pulses.
- **Important**: Significant state changes (e.g., GPS Fix acquired). Rendered in **Bold BrandJd**.
- **Special**: Forensic triggers. Rendered in **ForensicPink**.

... [Rest of document remains unchanged]

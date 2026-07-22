# Events and Logging Mechanism

This document describes how the system captures, processes, and displays forensic events.

## 🎨 Color Coding Standards (v9.4.0)
To ensure immediate situational awareness, the log and UI use a strict color-coding scheme aligned with the Branding Authority (R799e) and Forensic Visual Authority (R404b).

1.  **Critical / Error (Rose500 / Red)**: Triggered by `CRITICAL`, `ERROR`, `VIOLATION`, or active `SIREN` states.
2.  **Warning / Transition (Amber500 / Orange)**: Triggered by `TRACKER STATE` changes or signal degradation.
3.  **Success / Restored (BrandJd / JD Green #78BE20)**: Authoritative identity and success indicators.
4.  **Viewer / User Action (ViewerCyan / Cyan)**: Local user interactions.
5.  **Forensic Detail (Teal500 / Cyan)**: SNR snapshots and coordinate data in detail panes.
6.  **Forensic Special (ForensicPink #FF1493)**: Reserved for unique sensor triggers like `SITTING`, `VIBRATION`, or `TAMPER` (Requirement R404b).

## 📝 Logging Levels
- **Normal**: Standard telemetry pulses.
- **Important**: Significant state changes. Rendered in **Bold BrandJd**.
- **Special**: Forensic triggers (e.g., Sit Detection). Rendered in **ForensicPink**.

... [Rest of document remains unchanged]

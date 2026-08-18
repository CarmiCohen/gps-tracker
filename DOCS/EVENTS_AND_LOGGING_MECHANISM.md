# Events and Logging Mechanism (vAug.17.10)

This document describes how the system captures, processes, and displays forensic events. As of vAug.17.10, all terminology and locality references follow the R747 Locality Authority and R193 Persistence Authority.

## 🎨 Color Coding Standards
To ensure immediate situational awareness, the log and UI use a strict color-coding scheme aligned with the Branding Authority (R799e) and Forensic Visual Authority (R404b).

1.  **Critical / Error (Rose500 / Red)**: Triggered by `CRITICAL`, `ERROR`, `VIOLATION`, or active `SIREN` states.
2.  **Warning / Transition (Amber500 / Orange)**: Triggered by system state changes or signal degradation.
3.  **Success / Restored (BrandJd / JD Vivid Green #78BE20)**: Authoritative identity and success indicators.
4.  **Viewer / User Action (ViewerCyan / Cyan)**: Local user interactions.
5.  **Forensic Detail (Teal500 / Cyan)**: SNR snapshots and coordinate data in detail panes.
6.  **Forensic Special (ForensicPink #FF1493)**: Reserved for unique sensor triggers like `SITTING`, `VIBRATION`, or `TAMPER` (Requirement R404b).

## 📝 Logging Levels
- **Normal**: Standard telemetry pulses.
- **Important**: Significant state changes. Rendered in **Bold BrandJd**.
- **Special**: Forensic triggers (e.g., Sit Detection). Rendered in **ForensicPink**.

## 📍 Log Locality (R747 Authority)
To prevent ambiguity on budget hardware:
- **Viewer-Local Events**: Prefixed with "**This device:**" (e.g., *This device: Internet Lost*).
- **Tracker-Remote Events**: Redundant "Tracker:" prefixes are omitted. Descriptions use "**Device**" (e.g., *Device battery level is low*).

## 🗄️ Persistence & Storage (R193 Persistence Authority)
Events are stored in a high-performance memory-mapped circular buffer (`forensic_spill.bin`) before being drained to the SQLite database to prevent WAL pressure and UI stalls (Issue #669).

### Forensic Persistence Audit (R193)
The system performs a mandatory audit of the `forensic_spill.bin` signature during initialization. This audit ensures that:
1.  **Trace Retention**: Traces captured during high-frequency sampling windows are retained across process death, thermal reboots, or crashes.
2.  **Index Integrity**: The write and read indices of the circular buffer are correctly recovered from the file header.
3.  **Audit Trail**: The system logs the total number of recovered traces during startup to verify data continuity.

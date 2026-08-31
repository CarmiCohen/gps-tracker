# Events and Logging Mechanism (vAug.31.04)

This document describes how the system captures, processes, and displays forensic events. All terminology follows the R747 Locality Authority and R779 Sanitization Authority.

## 🎨 Color Coding Standards
To ensure immediate situational awareness, the log and UI use a strict color-coding scheme aligned with the Branding Authority (R799e) and Forensic Visual Authority (R404b).

1.  **Critical / Error (Rose500 / Red)**: Triggered by `CRITICAL`, `ERROR`, `VIOLATION`, or active `SIREN` states.
2.  **Warning / Transition (Amber500 / Orange)**: Triggered by system state changes or signal degradation.
3.  **Success / Restored (BrandJd / JD Vivid Green #78BE20)**: Authoritative identity and success indicators.
4.  **Viewer / User Action (ViewerCyan / Cyan)**: Local user interactions.
5.  **Forensic Detail (Teal500 / Cyan)**: SNR snapshots and coordinate data in detail panes.
6.  **Forensic Special (ForensicPink #FF1493)**: Reserved for unique sensor triggers like `SITTING`, `VIBRATION`, or `TAMPER` (Requirement R404b).

## 📝 Logging Levels & Sanitization (R779)
- **Normal**: Standard telemetry pulses.
- **Important**: Significant state changes. Rendered in **Bold BrandJd**.
- **Special**: Forensic triggers (e.g., Sit Detection). Rendered in **ForensicPink**.

### Forensic Metadata Sanitization (R779)
**MANDATORY**. All exported logs, trails, and telemetry payloads are processed via `ForensicSanitizer` before persistence or transmission.
1.  **Path Scrubbing**: Absolute internal storage paths (e.g., `/data/user/0/com.gps19.app/...`) are replaced with `[INTERNAL_PATH]`.
2.  **Hardware Normalization**: Device-specific identifiers (Model, Manufacturer, Board) are normalized to `[HW_ID]` unless explicitly marked as a forensic audit trace (`isSpecial`).
3.  **Audit Integrity**: Sanitization is applied at the logging edge and telemetry mapping layers to prevent accidental data leaks to external viewers or replayed history.

## 📍 Log Locality (R747 Authority)
To prevent ambiguity on budget hardware:
- **Viewer-Local Events**: Prefixed with "**This device:**" (e.g., *This device: Internet Lost*).
- **Tracker-Remote Events**: Redundant "Tracker:" prefixes are omitted. Descriptions use "**Device**" (e.g., *Device battery level is low*).

## 🗄️ Persistence & Storage (R193 Persistence Authority)
Events are stored in a high-performance memory-mapped circular buffer (`forensic_spill.bin`) before being drained to the SQLite database to prevent WAL pressure and UI stalls (Issue #669).

### Forensic Persistence Audit (R193)
The system performs a mandatory audit of the `forensic_spill.bin` signature during initialization to ensure trace retention across process death or crashes.

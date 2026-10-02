# SOT Master Requirements & Hardening Status (Oct.2.1)

## 🏗️ Architectural Master Rules (102 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.75 Coupled Alarm Signaling (R042/S576)**: The system MUST NOT trigger physical audio (siren) without an accompanying high-priority notification and visual context. (Oct.1.6).
*   **1.76 System-Wide Alarm Overlay (R578)**: Critical theft alarms MUST utilize `SYSTEM_ALERT_WINDOW` to guarantee UI promotion. (Oct.1.7).
*   **1.77 Global Alarm Acknowledgment (R185/S579)**: Alarm acknowledgment state MUST be synchronized globally. All triggers MUST be evaluated against `lastAlarmAckTs` carried in the telemetry stream to ensure idempotency across peer re-installs. (Oct.1.8 - Issue #1410).
*   **1.78 Mode-Based Alert Restriction (R588)**: Full-screen alert overlays MUST be restricted to Viewer mode only to preserve Tracker stealth. (Oct.1.8).
*   **1.79 Local State Authority (R589)**: Local diagnostic dashboards MUST reflect engine behavioral state independently of peer connectivity status. (Oct.1.8).
*   **1.80 Engine Map Atomicity (R585)**: All mutations to the shared alarm state map MUST be synchronized to prevent concurrent modification during high-frequency audits. (Oct.1.8).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 589**: Local State Routing - Fixed Tracker dashboard "UNKNOWN" state. (Resolved Oct.2.1).
*   **SOT ID 588**: Viewer-Only Alert Policy - Enforced mode-based restriction for AlarmOverlay. (Resolved Oct.2.1).
*   **SOT ID 585**: Engine Thread-Safety - Migrated to ConcurrentHashMap and synchronized mutations. (Resolved Oct.2.1).
*   **SOT ID 579**: Global Alarm Acknowledgment - Implemented `lastAlarmAckTs` propagation. (Resolved Oct.1.8).
*   **SOT ID 578**: System-Wide Alarm Overlay - Implemented `AlarmOverlayService`. (Resolved Oct.1.7).

---

## 🏁 Verification Chapters
*   **Chapter 31.210 (Thread-Safety Stress Audit)**: PASSED - Verified zero CME crashes during 250Hz sensor audit under CPU saturation. (Oct.2.1)
*   **Chapter 31.209 (Mode Isolation Audit)**: PASSED - Confirmed Red Alert suppression on Tracker devices. (Oct.2.1)
*   **Chapter 31.206 (Persistence Idempotency Audit)**: PASSED - Verified global `lastAlarmAckTs` sync. (Oct.1.8)

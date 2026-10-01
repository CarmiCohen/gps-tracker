# SOT Master Requirements & Hardening Status (Oct.1.8)

## 🏗️ Architectural Master Rules (95 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.75 Coupled Alarm Signaling (R042/S576)**: The system MUST NOT trigger physical audio (siren) without an accompanying high-priority notification and visual context. (Oct.1.6).
*   **1.76 System-Wide Alarm Overlay (R578)**: Critical theft alarms MUST utilize `SYSTEM_ALERT_WINDOW` to guarantee UI promotion. (Oct.1.7).
*   **1.77 Global Alarm Acknowledgment (R185/S579)**: Alarm acknowledgment state MUST be synchronized globally. All triggers MUST be evaluated against `lastAlarmAckTs` carried in the telemetry stream to ensure idempotency across peer re-installs. (Oct.1.8 - Issue #1410).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 579**: Global Alarm Acknowledgment - Implemented `lastAlarmAckTs` propagation to prevent recurring alerts on Viewer re-install. (Resolved Oct.1.8).
*   **SOT ID 578**: System-Wide Alarm Overlay - Implemented `AlarmOverlayService` for guaranteed alert visibility. (Resolved Oct.1.7).
*   **SOT ID 575**: Manual Silence Persistence - Enforced 5m duration via SirenLockoutUseCase. (Resolved Oct.1.6).

---

## 🏁 Verification Chapters
*   **Chapter 31.206 (Persistence Idempotency Audit)**: PASSED - Verified that alarms acknowledged on a Viewer are correctly suppressed on a fresh install after uninstallation, via global `lastAlarmAckTs` sync. (Oct.1.8)
*   **Chapter 31.205 (Overlay Hardening Audit)**: PASSED - Verified `AlarmOverlayService` promotes UI over third-party apps. (Oct.1.7)

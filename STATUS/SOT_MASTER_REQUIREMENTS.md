# SOT Master Requirements & Hardening Status (Oct19.19)

## 🏗️ Architectural Master Rules (299 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.225 Test Mode Restoration Bypass (R-ID 743)**: To allow deterministic testing of mode transitions and prevent "boot loops" into Tracker mode, the system MUST provide a persistent bypass for automatic service resumption (R-ID 242). This bypass MUST be toggleable via Diagnostics and respected by the MainViewModel during cold-start initialization. (Oct19.19 - ISSUE-OCT19.19-F1).
*   **1.224 Unified Network Callback Management (R-ID 742)**: To prevent redundant system overhead and log flooding, the system MUST maintain exactly one `ConnectivityManager.NetworkCallback` registration. This authority MUST reside in `NetworkProvider`. All other system status observers MUST consume the unified reactive stream provided by this provider. (Oct18.8 - ISSUE-OCT18.22-F2).
*   **1.223 Battery Saver Signaling Policy (R-ID 741)**: The system MUST explicitly account for system-wide Battery Saver (Power Save) mode in its signaling logic. Non-critical telemetry sync MUST be deferred when Battery Saver is active to preserve power, while ensuring immediate resumption if a security violation occurs. Forensic logs MUST reflect the unified "(Doze/PowerSave active)" state for auditability. (Oct18.8 - ISSUE-OCT18.22-F1).
... [Remaining rules preserved] ...

## 🛡️ Core Hardening Baseline
*   **SOT ID 773**: Test Mode Restoration Bypass - PASSED. Implemented persistent `isRestorationDisabled` flag and ViewModel hook to bypass R-ID 242. (Oct19.19 - ISSUE-OCT19.19-F1).
*   **SOT ID 772**: Unified Network Callback Management - PASSED. Consolidated all `ConnectivityManager` registrations into `AndroidNetworkProvider` and refactored `SystemStatusProviderImpl` to consume the unified flow. (Oct18.8 - ISSUE-OCT18.22-F2).
... [Remaining entries preserved] ...

# SOT Master Requirements & Hardening Status (Oct18.8)

## 🏗️ Architectural Master Rules (298 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.224 Unified Network Callback Management (R-ID 742)**: To prevent redundant system overhead and log flooding, the system MUST maintain exactly one `ConnectivityManager.NetworkCallback` registration. This authority MUST reside in `NetworkProvider`. All other system status observers MUST consume the unified reactive stream provided by this provider. (Oct18.8 - ISSUE-OCT18.22-F2).
*   **1.223 Battery Saver Signaling Policy (R-ID 741)**: The system MUST explicitly account for system-wide Battery Saver (Power Save) mode in its signaling logic. Non-critical telemetry sync MUST be deferred when Battery Saver is active to preserve power, while ensuring immediate resumption if a security violation occurs. Forensic logs MUST reflect the unified "(Doze/PowerSave active)" state for auditability. (Oct18.8 - ISSUE-OCT18.22-F1).
*   **1.222 Test Isolation Lifecycle (R-ID 740)**: To ensure deterministic behavior in multi-threaded test environments, repositories MUST provide hooks to manually trigger or inhibit background batch processors and drainers. (Oct18.8 - ISSUE-OCT18.18-F1).
*   **1.221 Doze Mode White-listing (R-ID 739)**: The system MUST allow telemetry signaling during Android Doze mode (Device Idle) if the application is explicitly white-listed by the user ("Ignoring Battery Optimizations"). This ensures high-persistence forensic continuity even when the OS attempts to defer network activity. (Oct18.8 - ISSUE-OCT18.16-F1).
*   **1.220 Urban Reflectivity Altitude Gating (R-ID 738)**: To harden stationary anchors against urban multipath, the system MUST apply extreme skepticism (0.25x breakout score multiplier) when a high-SNR reflection (SNR ≥ 35) is accompanied by a significant altitude shift (≥ 3.5m). (Oct18.8 - ISSUE-OCT18.14-F1).
*   **1.219 Acoustic Duty Cycle Overlap (R-ID 737)**: To ensure gapless capture of acoustic events during power-save duty cycles, the microphone MUST start recording at least 500ms (OVERLAP_MS) before the OFF cycle ends. (Oct18.8 - ISSUE-OCT18.12-F1).
*   **1.218 Steep-Drop Vibration Convergence (R-ID 736)**: The adaptive vibration floor MUST implement accelerated downward convergence (Steep-Drop) when the current vibration energy drops below 40% of the existing floor. (Oct18.8 - ISSUE-OCT18.10-F1).
*   **1.217 SNR Latch for Urban Multipath (R-ID 735)**: The system MUST implement a sustained security hold (SNR Latch) for location jumps that present with high SNR (SNR ≥ 35). (Oct18.8 - ISSUE-OCT18.8-F1).
*   **1.216 Synthetic Physical Validation (R-ID 734)**: The system MUST provide synthetic injection hooks for all physical hardware dependencies. (Oct18.8 - ISSUE-OCT18.6-F1).
*   **1.215 Explicit Component Lifecycle (R-ID 733)**: Core repositories and managers MUST NOT initiate background processing or side-effect heavy operations within their constructors. (Oct18.8 - SIMP-OCT18.2-S1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 772**: Unified Network Callback Management - PASSED. Consolidated all `ConnectivityManager` registrations into `AndroidNetworkProvider` and refactored `SystemStatusProviderImpl` to consume the unified flow. (Oct18.8 - ISSUE-OCT18.22-F2).
*   **SOT ID 771**: Battery Saver Signaling Policy - PASSED. Updated `ConnectivitySuite` logging to explicitly reflect `PowerSave` status in forensic traces, ensuring compliance with Chapter 38 audit signatures. (Oct18.8 - ISSUE-OCT18.22-F1).
*   **SOT ID 770**: Test Isolation Lifecycle - PASSED. Added `triggerManualBatchProcessor` and improved test synchronization in `StoragePressureAuditTest`. (Oct18.8 - ISSUE-OCT18.18-F1).
*   **SOT ID 769**: Doze Mode White-listing - PASSED. Implemented `isIgnoringBatteryOptimizations` check in `PowerStateProvider` and updated `HardwareSuite` to allow signaling bypass for white-listed sessions. (Oct18.8 - ISSUE-OCT18.16-F1).
*   **SOT ID 768**: Urban Signal Reflectivity Hardening - PASSED. Implemented 3.5m altitude gate skepticism in `AnchorEvaluator` for high-SNR reflections. (Oct18.8 - ISSUE-OCT18.14-F1).
*   **SOT ID 767**: Acoustic Duty Cycle Overlap - PASSED. Implemented 500ms transition buffering in `HardwareSuite.acousticThread`. (Oct18.8 - ISSUE-OCT18.12-F1).
*   **SOT ID 766**: Adaptive Vibration Floor Convergence - PASSED. Implemented Steep-Drop convergence logic. (Oct18.8 - ISSUE-OCT18.10-F1).
*   **SOT ID 765**: Urban Multipath Hardening - PASSED. Implemented 360s SNR Latch and high-SNR anchor skepticism. (Oct18.8 - ISSUE-OCT18.8-F1).
*   **SOT ID 764**: Synthetic Physical Injection - PASSED. Implemented `SyntheticEventInjector`. (Oct18.8 - ISSUE-OCT18.6-F1).

... [Remaining entries preserved] ...

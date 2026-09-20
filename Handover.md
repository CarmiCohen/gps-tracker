# Forensic Handover (Sep.15.101)

## 🎯 Current System State
*   **Version**: Sep.20.00 | **Build**: GNSS Throttling Jitter Logic Corrected
*   **Active Devices**: Samsung A15 & S21FE (Unified via PerformanceTier)
*   **SOT Baseline**: SOT-373 (GNSS Jitter Accuracy)

## 🛡️ Forensic Hardening (Session Summary)

### 1. GNSS Jitter Audit Correction (#1120)
*   **Status**: Resolved in Sep.20.00.
*   **Remediation**:
    *   **ForensicAuditor.kt**: Updated `recordGnssStatus` to accept a dynamic `expectedIntervalMs`. Jitter is now calculated relative to the active sampling rate.
    *   **HardwareSuite.kt**: Updated `gnssStatusCallback` to pass the throttled interval (2s or 5s) to the auditor.
*   **Verification**: PASSED (Chapter 31.37). Spurious stability alerts during cooling cycles are eliminated.

## 🔴 Open Gaps (Resumption Points)

### 1. ViewerService Local Hardware Leak (#1121)
*   **File**: `ViewerService.kt` (Line 458)
*   **Detail**: Remote alarm evaluation uses local SNR instead of remote Tracker SNR.
*   **Impact**: Inaccurate Jammer/Stall detection for remote devices.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 373 (Rules: 77, IDs: 373), Resolved: 1120, Open: 1, Testing: 2 (Sub-items: 10), Ideas: 19, QA: 282]**

**Resumption Context**: Forensic stability auditing is now performance-tier aware. The next step is decoupling local hardware state from remote telemetry processing in `ViewerService`.

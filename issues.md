# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.24.00

## 🎯 Current Resumption Focus: Background Infrastructure Hardening
Finalizing the audit of background service stability and functional convergence after the role-isolation refactor.

## 🔴 Open Gaps & Unfinished Integration Points

### Background Service Infrastructure & Hardening Gaps (Rigorous Audit of #1171)

*   **Issue #1233: High Allocation Churn via Fast-Path Re-registration**
    *   *Description*: Fast-path callbacks are re-registered on every service tick.
    *   *Significance*: **Medium (CPU Tuning)**.

*   **Issue #1255: Unreliable Monotonic Clock Recovery Across Reboots**
    *   *Description*: Monotonic drift reference is invalid across reboots.
    *   *Significance*: **Medium-High (Forensic Integrity)**.

### Core Integration Gaps

*   **Issue #1271: Missing Persistence for Adaptive Vibration Floor**
    *   *Description*: Vibration floor resets on service restart.
    *   *Significance*: **High (False Positive Risk)**.

*   **Issue #1273: Atomic User Counter Risk in HardwareSuite**
    *   *Description*: Negative counter values prevent hardware shutdown.
    *   *Significance*: **Medium (Resource Leak)**.

---

## 💡 Strategic Simplification Ideas (Ideas: 17)

*   **Issue #1295: Redundant Stream Observer Audit** (Low): The duplicate heartbeat issue in `ViewerService` suggests a need for a systematic audit of all `BaseMonitorService` descendants to ensure no other redundant reactive streams are active, further reducing CPU wakeups.
*   **Issue #1294: Build-Time Interface Validation** (Medium): Implement a custom Gradle task to verify that all `BaseMonitorService` descendants implement the full abstract interface before compilation, preventing the "unimplemented member" regressions seen in Sep.23.71.
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator** (Low-Medium)
*   **Issue #1292: Reactive Siren State Binding** (High)
*   **Issue #1291: Domain Event Bus Integration** (High)

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1232: Empty Stub Implementation of OEM Power Hardening Overrides** (Resolved Sep.24.00)
    *   *Remediation*: Converted cosmetic stubs in `DeviceHardeningStrategy.kt` into functional hardening logic. Implemented Samsung and Huawei-specific WakeLock escalation and Watchdog re-alignment to prevent OS-level service termination (R-ID 421).
*   **Issue #1231: Redundant Stream Overlap & Duplicate Heartbeat Processing in ViewerService** (Resolved Sep.23.80)
    *   *Remediation*: Removed redundant `observeHistoryEvents` and consolidated peer pulse handling to ensure idempotent processing (R-ID 456).
*   **Issue #1250: Build Vitality & Reactive Stream Convergence** (Resolved Sep.23.72)
    *   *Remediation*: Implemented missing abstract members in `TrackerService`, fully hydrated `MainRepository` delegates for draft settings, and corrected `MainViewModel` flow typing to restore `.value` access.
*   **Issue #1230: Shared Storage Key Leakage & Cross-Role State Corruption** (Resolved Sep.23.70)
*   **Issue #1240: Role-Based Namespace Isolation for Logic State Persistence** (Resolved Sep.23.70)
*   **Issue #1236: Race Conditions and Premature Tick Execution** (Resolved Sep.23.70)

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 456 (Rules: 92, IDs: 456), Resolved: 1193, Open: 20, Testing: 3 (Sub-items: 12), Ideas: 17, QA: 284]**

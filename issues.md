# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.23.70

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Background Service Infrastructure & Hardening Gaps (Rigorous Audit of #1171 & Service Overlays)

*   **Issue #1231: Redundant Stream Overlap & Duplicate Heartbeat Processing in ViewerService**
    *   *Description*: `ViewerService.kt` contains a copy-paste duplication error where heartbeat events are processed twice.
    *   *Significance*: **Medium-High (Logic Bug & Telemetry Churn)**.

*   **Issue #1232: Empty Stub Implementation of OEM Power Hardening Overrides**
    *   *Description*: `DeviceHardeningStrategy.kt` contains purely cosmetic log messages instead of actual adaptation rules.
    *   *Significance*: **High (Stay-Alive Reliability Risk)**.

*   **Issue #1233: High Allocation Churn & GC Pressure via Fast-Path Re-registration**
    *   *Description*: Fast-path callbacks are re-registered on every service tick.
    *   *Significance*: **Medium (Performance & CPU Tuning)**.

*   **Issue #1234: Faulty Latency Calculations inside Thermal Recovery Audits**
    *   *Description*: Latency audit logs the delta on the immediate subsequent loop iteration, measuring only one loop delay.
    *   *Significance*: **Low-Medium (Audit Reliability)**.

*   **Issue #1235: Synchronous Main-Thread runBlocking Invocation in Service Shutdown**
    *   *Description*: Blocks service teardown during termination.
    *   *Significance*: **Medium (Lifecycle Reliability)**.

*   **Issue #1255: Unreliable Monotonic Clock Recovery Across Reboots**
    *   *Description*: Monotonic drift reference is invalid across reboots.
    *   *Significance*: **Medium-High (Temporal Forensic Integrity)**.

*   **Issue #1256: Monotonic Latch Staleness Across Reboots**
    *   *Description*: ElaspedRealtime latches from previous boots block sirens.
    *   *Significance*: **High (Alarm Reliability & Safety Side Effect)**.

### Missing Functionality & Core Integration Gaps (Identified Resolved Item Failures)

*   **Issue #1271: Missing Persistence for Adaptive Vibration Floor**
    *   *Description*: Vibration floor resets to default on service restart.
    *   *Significance*: **High (False Positive Risk)**.

*   **Issue #1272: Alarm Notification Leak in Tracker Mode**
    *   *Description*: Siren triggers immediately after switching roles if an alarm was active in Tracker mode.
    *   *Significance*: **Medium (UX Side Effect)**.

*   **Issue #1273: Atomic User Counter Risk in HardwareSuite**
    *   *Description*: Negative counter values prevent hardware shutdown.
    *   *Significance*: **Medium (Resource Leak)**.

### Required Remediation & Strategic Implementation (Strategic Recommendations for Resolution)

#### For Issue #1171 (Service & Background Hardening)
*   **Issue #1241: Functional Restoration of History Sync Streams in ViewerService** (High)
*   **Issue #1242: Operationalization of OEM Power Hardening Logic** (High)
*   **Issue #1243: Fast-Path Binding Lifecycle Optimization** (Medium)
*   **Issue #1244: Heuristic Correction for Thermal Recovery Audits** (Low-Medium)
*   **Issue #1245: Non-Blocking History Flush on Service Termination** (Medium)
*   **Issue #1246: Critical Section Enforcement during Service Initialization** (High)
*   **Issue #1260: Boot-ID Validation for Persistent Monotonic Latches** (High)
*   **Issue #1261: Refactor Tracker/Viewer Services into Role-Reactive MonitorService** (Medium)

#### For Alarm & Siren Reliability (Core System Restoration)
*   **Issue #1281: Implementation of Persistent Adaptive Vibration Floor** (High)
*   **Issue #1282: Atomic Guard for HardwareSuite User Counter** (Medium)
*   **Issue #1265: Unified Event Orchestration via AppEventCoordinator** (Medium)

---

## 💡 Strategic Simplification Ideas (Ideas: 15)

*   **Issue #1293: Lifecycle-Aware Tick Orchestrator** (Low-Medium): Refactor `BaseMonitorService` to use a dedicated TickOrchestrator that handles the initialization gate internally, removing the need for manual `await()` calls in background loops.
*   **Issue #1292: Reactive Siren State Binding** (High): Move siren lifecycle orchestration into a dedicated `SirenService` or `SirenCoordinator` to remove the imperative `audioSynthesizer.play/stop` calls from the evaluation manager.
*   **Issue #1291: Domain Event Bus Integration** (High): Centralize dispersed logging, telemetry capture triggers, and command routing into a single `AppEventCoordinator` to eliminate cross-component coupling.
*   **Issue #1161: Unified Trajectory & Buffer Management** (Medium-High)
*   **Issue #1290: UI State Mapper Consolidation** (Medium): Merge `UiStateMapper` logic directly into `MainViewModel` now that it is the sole activity-scoped consumer, reducing DI surface area.
*   **Issue #1172: Smart Signaling Dispatcher** (Medium)
*   **Issue #1163: Stateless & Functional Logic Refactoring** (Medium)
*   **Issue #1160: Flyweight & Pooling Expansion** (Medium)
*   **Issue #1173: Protobuf-First Persistence** (Medium)
*   **Issue #1205: Context-Aware Power Optimization** (Medium)
*   **Issue #1201: Reactive Siren Lockout** (Medium)
*   **Issue #1202: UI Event Routing Unification** (Medium)
*   **Issue #1167: Map Overlay Imperative to Declarative Controller** (Low-Medium)
*   **Issue #1171: Service & Worker Consolidation** (Low)
*   **Issue #1175: Real-time Only Path (Pivot Option)** (Strategic)

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1230: Shared Storage Key Leakage & Cross-Role State Corruption** (Resolved Sep.23.70)
*   **Issue #1240: Role-Based Namespace Isolation for Logic State Persistence** (Resolved Sep.23.70)
*   **Issue #1236: Race Conditions and Premature Tick Execution during Asynchronous Initialization** (Resolved Sep.23.70)
*   **Issue #1270: Disconnected Siren Trigger Mechanism** (Resolved Sep.23.60)
*   **Issue #1280: Integrate Siren Trigger into Alarm Evaluation Loop** (Resolved Sep.23.60)
*   **Issue #1203: Hilt ViewModel Scope Optimization** (Resolved Sep.23.50)
    *   *Sub-issues Resolved*: #1210 (Fragmented State), #1211 (Stream Churn), #1212 (Map State Loss), #1213 (Swallowed Events), #1214 (Hydration Races), #1215 (Scope Overlap), #1257 (Misrouted Kinematic State).
*   **Issue #1204: Unified Hardware Lifecycle & Vendor Hardening** (Resolved Sep.23.08)
*   **Issue #1164: Persistence of Logic State** (Resolved Sep.23.06)
*   **Issue #1200: Shared Overlay Scope** (Resolved Sep.23.04)
*   **Issue #1192: Disconnected Settings Input State Flow** (Resolved Sep.23.03)
*   **Issue #1193: Asymmetric Audio Control and Siren State Dispersion** (Resolved Sep.23.01)

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 425 (Rules: 89, IDs: 425), Resolved: 1190, Open: 23, Testing: 3 (Sub-items: 12), Ideas: 15, QA: 283]**

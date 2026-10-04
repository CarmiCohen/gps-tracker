# SOT Master Requirements & Hardening Status (Oct.4.1)

## 🏗️ Architectural Master Rules (125 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.100 Smart Signaling Dispatcher (R1172)**: ALL signaling triggers (including joins, pings, and telemetry) MUST be routed through a unified `SmartSignalingDispatcher`. The dispatcher MUST enforce connection-aware delivery and adaptive throttling, ensuring high-priority commands bypass inter-frame delays while telemetry is conflated and throttled to preserve bandwidth. (Oct.3.8 - Issue #1172).
*   **1.101 Reactive Siren Lockout (R1201)**: Siren lockout and cooldown authority MUST be centralized in a dedicated `SirenLockoutUseCase`. The tracking engine MUST NOT maintain its own lockout state, instead consuming a reactive boolean from the UseCase to ensure architectural decoupling between domain policy and audio generation. (Oct.3.9 - Issue #1201).
*   **1.102 UI Event Routing Unification (R1202)**: ALL imperative UI actions (navigation, service startup, permission requests) MUST be delivered via a unified `UiEffect` stream from the `UiEventCoordinator`. ViewModels and Composables MUST NOT execute procedural orchestration logic directly; instead, they MUST delegate to the coordinator to ensure strict decoupling between business policy and UI framework implementation. (Oct.4.1 - Issue #1202).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 612**: UI Event Routing Unification - Refactored navigation and procedural UI logic into a centralized domain coordinator with a reactive effect stream. (Resolved Oct.4.1).
*   **SOT ID 610**: Reactive Siren Lockout - Decoupled siren cooldown logic from audio generation and violation detection into a centralized UseCase. (Resolved Oct.3.9).
*   **SOT ID 608**: Smart Signaling Dispatcher - Consolidated all signaling triggers into a reactive coordination layer with adaptive throttling and conflation. (Resolved Oct.3.8).
*   **SOT ID 607**: StatusBar Visual & Logic Hardening - Fixed portrait overflow, color inconsistency, and clock source mismatch. (Resolved Oct.3.7).

---

## 🏁 Verification Chapters
*   **Chapter 31.231 (UI Routing Audit)**: PASSED - Verified that `MainAppContent` is now a passive receiver of `UiEffect`. Confirmed `UiEventCoordinator` correctly orchestrates permission checks and service startup delays. Validated that ViewModels no longer hold references to screen routes. (Oct.4.1)
*   **Chapter 31.230 (Siren Lockout Audit)**: PASSED - Verified that `MainAlarmLogic` is now stateless regarding siren cooldowns. Confirmed `SirenLockoutUseCase` correctly manages manual silences and auto-stop cooldowns. Validated state recovery across service restarts. (Oct.3.9)
*   **Chapter 31.228 (Smart Signaling Audit)**: PASSED - Verified connection-aware delivery for all commands. Confirmed adaptive throttling allows join/ping bursts while maintaining inter-frame delays for location updates. (Oct.3.8)

# SOT Master Requirements & Hardening Status (Oct.5.1)

## 🏗️ Architectural Master Rules (129 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.100 Smart Signaling Dispatcher (R1172)**: ALL signaling triggers (including joins, pings, and telemetry) MUST be routed through a unified `SmartSignalingDispatcher`. The dispatcher MUST enforce connection-aware delivery and adaptive throttling, ensuring high-priority commands bypass inter-frame delays while telemetry is conflated and throttled to preserve bandwidth. (Oct.3.8 - Issue #1172).
*   **1.101 Reactive Siren Lockout (R1201)**: Siren lockout and cooldown authority MUST be centralized in a dedicated `SirenLockoutUseCase`. The tracking engine MUST NOT maintain its own lockout state, instead consuming a reactive boolean from the UseCase to ensure architectural decoupling between domain policy and audio generation. (Oct.3.9 - Issue #1201).
*   **1.102 UI Event Routing Unification (R1202)**: ALL imperative UI actions (navigation, service startup, permission requests) MUST be delivered via a unified `UiEffect` stream from the `UiEventCoordinator`. ViewModels and Composables MUST NOT execute procedural orchestration logic directly; instead, they MUST delegate to the coordinator to ensure strict decoupling between business policy and UI framework implementation. (Oct.4.1 - Issue #1202).
*   **1.103 Unified Clock Authority (R1425)**: ALL system-internal temporal evaluations, including telemetry "freshness", HUD age reporting, service execution intervals, and behavioral heuristics (e.g., stationary lockout), MUST strictly utilize monotonic time (`SystemClock.elapsedRealtime()`). The Wall Clock (`currentTimeMillis`) MUST be reserved exclusively for absolute audit logging and human-readable UI timestamps to prevent logic corruption during NTP syncs or user clock adjustments. (Oct.4.5 - Issue #1425).
*   **1.104 Telemetry Pooling & Flyweight Authority (R1160)**: HIGH-FREQUENCY telemetry entities (LocationUpdate, ProcessedLocation, TrajectoryNode) MUST be acquired from centralized ring-buffered pools (`EnginePools`). Direct allocation of telemetry snapshots within the evaluation loop is FORBIDDEN to ensure zero-allocation performance and thread-safety during reactive event emission. (Oct.4.6 - Issue #1160).
*   **1.105 Logic State Serialization (R1201-1)**: Alarm evaluation state (geofence counters, violation timestamps, lockout heuristics) MUST be consolidated into a single binary Protobuf blob (`LogicStateProto`) for DataStore persistence. Monotonic clock recovery MUST be validated against `boot_id` during restoration to ensure forensic continuity across service restarts. (Oct.5.1 - Issue #SIMP-1201-1).
*   **1.106 Protobuf-First Persistence (R1173)**: Connection history and pending updates MUST prioritize binary restoration from Protobuf payloads (`payload` BLOB column). Legacy SQLite columns MUST be treated as metadata for indexing only. Retrieval pipelines MUST implement on-the-fly binary migration for legacy entries to ensure 100% field parity during restoration. (Oct.5.1 - Issue #1173).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 616**: Protobuf-First Persistence (Phase 2) - Completed binary migration path and established binary-first restoration for history and pending updates. (Resolved Oct.5.1).
*   **SOT ID 615**: Logic State Serialization Expansion - Consolidated alarm evaluation state into binary DataStore blobs with session-aware monotonic recovery. (Resolved Oct.5.1).
*   **SOT ID 614**: Telemetry Pooling & Flyweight Expansion - Integrated ring-buffered pools for all high-frequency engine entities to eliminate GC churn and allocation overhead. (Resolved Oct.4.6).
*   **SOT ID 613**: Unified Clock Authority - Standardized all internal logic and UI freshness arithmetic to monotonic time sources via `TimeProvider`. (Resolved Oct.4.5).
*   **SOT ID 612**: UI Event Routing Unification - Refactored navigation and procedural UI logic into a centralized domain coordinator with a reactive effect stream. (Resolved Oct.4.1).
*   **SOT ID 610**: Reactive Siren Lockout - Decoupled siren cooldown logic from audio generation and violation detection into a centralized UseCase. (Resolved Oct.3.9).
*   **SOT ID 608**: Smart Signaling Dispatcher - Consolidated all signaling triggers into a reactive coordination layer with adaptive throttling and conflation. (Resolved Oct.3.8).

---

## 🏁 Verification Chapters
*   **Chapter 31.235 (Binary Persistence Audit)**: PASSED - Verified binary payload restoration for history ribbons. Confirmed legacy column fallback is only used for metadata. Validated on-the-fly migration for legacy offline buffer entries. (Oct.5.1)
*   **Chapter 31.234 (Logic Serialization Audit)**: PASSED - Verified geofence counter and violation timestamp preservation across service restarts. Confirmed `boot_id` correctly triggers monotonic reset on hard reboot. (Oct.5.1)
*   **Chapter 31.233 (Pooling & GC Audit)**: PASSED - Verified zero allocations for `LocationUpdate` and `ProcessedLocation` during high-frequency evaluation loops. Confirmed `TrajectoryNode` reuse in `GtoEngine` promotion path. (Oct.4.6)
*   **Chapter 31.232 (Clock Authority Audit)**: PASSED - Verified that all age-based UI elements and service loop intervals now use monotonic deltas. Confirmed that NTP sync simulations do not trigger spurious "GPS Stall" or "Telemetry Stale" alerts. Validated that `LogRepository` flush timers are no longer susceptible to wall-clock drift. (Oct.4.5)
*   **Chapter 31.231 (UI Routing Audit)**: PASSED - Verified that `MainAppContent` is now a passive receiver of `UiEffect`. Confirmed `UiEventCoordinator` correctly orchestrates permission checks and service startup delays. (Oct.4.1)
*   **Chapter 31.230 (Siren Lockout Audit)**: PASSED - Verified that `MainAlarmLogic` is stateless regarding siren cooldowns. (Oct.3.9)

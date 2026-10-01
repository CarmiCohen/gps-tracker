# SOT Master Requirements & Hardening Status (Oct.1.3)

## 🏗️ Architectural Master Rules (84 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.61 Reactive Camera Actions (R726/S558)**: Imperative map camera animations (zoom, centering) MUST be delivered via a `SharedFlow<CameraAction>` rather than cumulative state triggers. This decouples transient UI commands from the persistent `MapViewState`, eliminating state churn and ensuring that camera events are processed exactly once per emission (Issue #1390).
*   **1.62 Scale-Aware Ribbon Layouts (R727/S559)**: Forensic ribbons and time rulers MUST utilize reserved bottom-padding and orientation-aware height metrics to prevent timestamp truncation and ensure legibility across budget hardware viewports (Issue #1384).
*   **1.63 Centralized Behavioral Authority (R-ID 548/S560)**: The high-level behavioral state of the tracker (`TrackerState`) MUST be determined exclusively within the engine's primary tick loop (`MonitorService`) via `TrackerStateManager`. Downstream consumers (HUD, Signaling, Persistence) MUST utilize the state value captured in the `SystemEvaluationSnapshot` to ensure global consistency and prevent velocity-state desynchronization (Issue #1386).
*   **1.64 Peer Relay Argument Robustness (R-ID 549/S561)**: Signaling relay handlers MUST adaptively support both single-argument and multi-argument (routingId-prefixed) payloads to ensure link stability across varying Socket.io relay server configurations (Issue #1385).
*   **1.65 Unified Role Identity Authority (R-ID 565/S565)**: The system MUST utilize a central `AppRole` enum for all role-based logic. The use of manual string prefixes (`T_`, `V_`, `VR_`) for repository keys or signaling identification is strictly forbidden to prevent contract fragility (Issue #1406).
*   **1.66 Persistent Alarm Lockout (R-ID 566/S566)**: Manual siren silences and cooldowns MUST be persisted to the database and remain valid across application restarts and device reboots. The `lastSirenStopRt` must not be wiped upon `boot_id` changes (Issue #1404).
*   **1.67 Standardized Dismissal Lockout (R-ID 567/S567)**: Manual user dismissal of an alarm MUST engage a minimum 30-second siren lockout period, as defined in the Alarming SOT (Issue #1403).
*   **1.68 Unified Storage Authority (R-ID 568/S568)**: All namespaced persistent storage operations MUST utilize the `AppRole` enum parameter within the `SettingsRepository` API. Manual string concatenation for role-based key prefixing is strictly prohibited to ensure type safety and namespace integrity (Issue #1407).
*   **1.69 Root-Level Thermal Persistence (R-ID 569/S569)**: The forced `COOLING_MODE` state and its entry timestamp MUST be persisted to the root `AppSettings` namespace. This ensures that thermal mitigation remains active across service restarts and that recovery latency can be accurately audited from the `coolingEnteredRt` baseline (Issue #1408).
*   **1.70 Descending Prefix Resolution (R-ID 570/S570)**: The `AppRole.fromKey` resolution MUST utilize descending-length evaluation (checking `VR_` before `V_`) to prevent namespace collisions. Failure to do so risks state leakage between local viewer settings and remote tracker telemetry (Issue #1408).
*   **1.71 Unified Alarm Authority Parity (R-ID 571/S571)**: The `MainRepository` MUST monitor the `AppRole.VIEWER_REMOTE` partition for alarm acknowledgments when in Viewer mode. This ensures consistency between the HUD's acknowledgment state and the `AppAlarmManager`'s evaluation logic (Issue #1408).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 571**: Unified Alarm Authority Parity - Synced MainRepository acknowledgment flow with VR_ partition. (Resolved Oct.1.3).
*   **SOT ID 570**: Descending Prefix Resolution - Enforced length-priority in AppRole fromKey mapping. (Resolved Oct.1.3).
*   **SOT ID 569**: Root-Level Thermal Persistence - Integrated cooling state into root DataStore schema. (Resolved Oct.1.3).
*   **SOT ID 568**: Unified Storage Authority - Refactored SettingsRepository to utilize AppRole-based API overloads. (Resolved Oct.1.2).
*   **SOT ID 567**: Standardized Dismissal Lockout - Enforced 30s global lockout in EngineConstants. (Resolved Sep.30.43).

---

## 🏁 Verification Chapters
*   **Chapter 31.201 (Thermal Persistence Audit)**: PASSED - Verified COOLING_MODE and coolingEnteredRt are correctly recovered after service restart during simulated heat event. (Oct.1.3)
*   **Chapter 31.202 (Prefix Isolation Audit)**: PASSED - Verified AppRole.fromKey correctly distinguishes between "V_TEST" and "VR_TEST" using length-descending evaluation. (Oct.1.3)
*   **Chapter 31.200 (Storage Authority Audit)**: PASSED - Verified all role-prefixed DataStore calls utilize the `AppRole` parameter; manual `"prefix" + key` concatenation eliminated in `MonitorService`, `AlarmManager`, and `HistoryManager`. (Oct.1.2)
*   **Chapter 31.199 (Role Contract Audit)**: PASSED - Verified all modules utilize AppRole enum; no hardcoded "T_"/"V_"/"VR_" prefixes remain in active code. (Sep.30.43)

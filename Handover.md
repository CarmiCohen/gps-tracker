# Forensic Handover - v9.2.0 (HUD Freshness & Hardening)

## 📌 Status: Stable / Build PASS / UI Hardened
This cycle addresses HUD visibility issues by decoupling telemetry connection state from GPS fix age.

### 🟢 Completed: Requirement R987 (HUD Telemetry Freshness)
*   **Critical Remediation (Issue #048)**: 
    *   Differentiated "Telemetry Age" from "GPS Fix Age" in `SharedUiComponents.kt`.
    *   Refactored `StatusRowData` to maintain colorization for Battery, Temperature, Signal (CommBar), and Satellite counts as long as the telemetry link is fresh.
    *   Preserved color for Distance indicator based on last known good position while telemetry is active.
    *   Strictly isolated "Grayout" (Slate500) logic to position-dependent fields (Accuracy, GPS Age) during signal staleness.

### 🟢 Completed: Requirement R986 (Binary Parity Gap Closure)
*   **Critical Remediation (Issue #051)**: 
    *   Synchronized `RealtimeStatus` Protobuf definition with `TrackerStatus` model.
    *   Added `tracker_state`, `is_anchor_locked`, `is_location_pending`, `location_pending_reason`, and hardware status fields to binary pulses.
    *   Hardened `CommunicationManager.handleLocationRelayBinary` to adopt these fields into the authoritative state flow.

### 🟢 Completed: Requirement R985 (Authoritative State Flow)
*   **Remediation (Issue #046)**: Tracker-side behavioral computation is now the source of truth. Viewer adopts `tracker_state` directly from telemetry.
*   **Remediation (Issue #047)**: Standardized speed unit to m/s across the entire pipeline. Hardened UI with freshness gates to prevent ghost speed updates during signal loss.

### 🛠 Instructions for Resumption
1.  **Verification of #048**: Run the app in Viewer mode. Move the Tracker indoors (to lose GPS) but keep it online. Verify that Battery, Temp, and Signal remain green/colorized while only Accuracy and GPS Age turn gray.
2.  **Binary Pulse Test**: Verify state adoption (MOVING/PARKING) when receiving binary packets.
3.  **Soak Test**: Monitor for HUD LED contradictions (#044) in prolonged sessions.

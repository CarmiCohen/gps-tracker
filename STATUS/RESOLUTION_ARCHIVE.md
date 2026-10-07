# 📜 Resolution Archive

## 🟢 Resolved in Oct7.1
*   **Issue #SIMP-1006-14: Telemetry Field Pruning.**
    *   **Root Cause Remediation**: Separated engine-internal evaluation state from transmission state in the `LocationUpdate` monolith.
    *   **Architectural Hardening**: Marked scratchpad fields (`snrSnapshot`, `vibeSnapshot`, `suppressionNote`) and tick-local synchronization fields (`nowRt`, `nowTs`, `isMuzzled`, etc.) as `@Transient`. This ensures they are excluded from JSON serialization (used in signaling and secondary logging), reducing wire payload size and clarifying the API boundary for external consumers.

## 🟢 Resolved in Oct6.23
*   **Issue #QA-1006-12: Signaling Efficiency Hardening.**
    *   **Root Cause Remediation**: Identified that log conflation windows were fixed and did not adapt to burst pressure, causing premature flushes.
    *   **Architectural Hardening**: Refactored `SmartSignalingDispatcher` to implement dynamic conflation window scaling for logs, synchronized with the telemetry bucket strategy. This ensures 100Hz bursts are correctly conflated into high-density packets.
*   **Issue #SIGN-1006-13: Conflation Strategy Consolidation.**
    *   **Root Cause Remediation**: Reduced cross-module coupling by migrating field-level conflation logic from `SignalingMessageConflator` utility into internal `SignalingPipeline` handlers.
    *   **Cleanup**: Removed the deprecated `SignalingMessageConflator.kt` utility.

## 🟢 Resolved in Oct6.20
*   **Issue #SIGN-1006-12: SignalingPipeline Abstraction.**
    *   **Root Cause Remediation**: Decoupled Protobuf serialization, delta-encoding state, and Gzip compression from `CommunicationManager` by creating a dedicated `SignalingPipeline` component.
    *   **Architectural Hardening**: Created `SignalingPipeline` interface and `SignalingDeltaState` in `:core:engine`. Implemented `AppSignalingEncoder` in `:app` to encapsulate wire-level transformations. Refactored `SmartSignalingDispatcher` to manage the unified pipeline lifecycle, ensuring delta-state is reset on reconnection (Rule 1.131).

## 🟢 Resolved in Oct6.15
*   **Issue #AUDIT-1006-11: Protobuf Stream Compression.**
...

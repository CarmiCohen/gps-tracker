# Issue #1417: Jitter-Resistant Connectivity Transitions

## 🎯 Objective
Harden the state machine transitions between `RELAY_OFFLINE` and `SIGNAL_LOSS` to prevent UI oscillation and false re-triggering caused by 500ms relay jitter.

## 🚩 Problem Statement
During the **Oct.1.8** soak tests, network jitter on cross-continental links (500ms lag) caused the system to rapidly oscillate between "Relay Lost" and "Signal Loss" states. This results in erratic UI behavior and log spam, even when the underlying peer connectivity is theoretically stable.

## 🛠️ Proposed Mediation
*   **Temporal Hysteresis**: Implement a 3-second "Confirmation Window" before promoting a relay drop to a `SIGNAL_LOSS` violation.
*   **Monotonic Recovery**: Ensure that once a connection is re-established, the "Resolved" signal is held for at least 2 seconds before allowing a new "Lost" state to trigger.
*   **Debounced Mapping**: Use the `TelemetryMapper` to filter out out-of-order latency packets that arrive within the jitter window.

## 📊 Requirements
*   **R-ID 594**: Connectivity violations MUST be debounced by a minimum of 3000ms to ignore standard relay jitter.
*   **R-ID 595**: The transition from `RELAY_OFFLINE` to `SIGNAL_LOSS` MUST be monotonic within a single evaluation cycle.

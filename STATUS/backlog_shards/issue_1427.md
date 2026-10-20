# Issue #1427: Asymmetrical Connection State (Red VWR Badge)

## 🎯 Objective
Resolve the logic error where the Tracker device fails to recognize an active Viewer connection (remaining Red), even when the Viewer correctly sees the Tracker (Green).

## 🚩 Problem Statement
In `ConnectivitySuite`, the logic gate for processing peer pulses was role-restricted, preventing the Tracker from calling `setTrackerConnected(true)` upon receiving a `viewer_pulse`. This resulted in a persistent "Red" connection badge on the Tracker despite active signaling.

## 🛠️ Proposed Mediation
*   **Unified Pulse Handling**: Remove the role restriction in `processJsonUpdateInternal` for pulse events to ensure both modes update peer connection state upon any valid signaling activity.

## 📊 Requirements
*   **R-ID 605**: The Tracker MUST transition the VWR badge to Green/Cyan upon receipt of a valid `viewer_pulse` or `pong_activity`.

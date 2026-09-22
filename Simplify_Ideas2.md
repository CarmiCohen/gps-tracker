# Strategic Architectural Simplification Ideas (Part 2)

## 💡 Idea #1: Generic Fast-Path Snapshotting
*   **Description**: Group baseline and spike data into the `ForensicSnapshot` itself. This allows the background service to adjust baselines atomically without redundant `synchronized(this)` blocks in `HardwareSuite`.
*   **Benefit**: Simplifies the parameter flow between `HardwareSuite` and `LocationProcessor` while reducing synchronization overhead on the sensor thread.

## 💡 Idea #2: Trigger-Based Forensic Sampling
*   **Description**: Transition from a fixed-interval forensic loop to a "Signal-on-Spike" model where `HardwareFastPath` triggers a telemetry capture only when a physical boundary is crossed or a significant delta is detected.
*   **Benefit**: Reduces background CPU wakeups and GC pressure by eliminating redundant data points during long stationary periods.

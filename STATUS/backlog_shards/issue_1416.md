# Issue #1416: Memory Pressure Mitigation for Forensic Auditor

## 🎯 Objective
Implement an aggressive memory-flush and buffer-throttling strategy for the `ForensicAuditor` to prevent Out-Of-Memory (OOM) fatal exceptions on low-memory devices like the A15.

## 🚩 Problem Statement
The **Oct.1.8** high-frequency sensor audit (250Hz) generates significant telemetry data. During sustained 1-hour alert cycles, the in-memory log buffer and forensic trace segments can consume a large portion of the available heap on the A15, especially when disk I/O is throttled.

## 🛠️ Proposed Mediation
*   **Adaptive Throttling**: Reduce sampling frequency from 10ms to 100ms automatically if `heapAllocatedMb` exceeds a defined threshold (e.g., 80% of max heap).
*   **Urgent Spill**: Force a binary spill-to-disk of the `ForensicSpillBuffer` when `isStorageLow` is false but heap pressure is high.
*   **Conflation Override**: Aggressively conflate non-critical telemetry points during periods of high memory pressure.

## 📊 Requirements
*   **R-ID 592**: The system MUST reduce forensic sampling rate when heap headroom drops below 15% to maintain engine continuity.
*   **R-ID 593**: Memory-based pressure signals MUST trigger a prioritized log flush.

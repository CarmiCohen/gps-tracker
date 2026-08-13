# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 588**

## 27. Samsung A15 Detection Hardening (Aug.13.04)
*   **Issue #150: Samsung A15 Phone Setup Bypass**.
    *   **Resolution**: Hardened the **Samsung A15 Detection Logic (R405)**. Broadened the `isA15Device` detection in `SystemStatusProvider` to inspect `Build.DEVICE` and `Build.PRODUCT` strings, capturing variants like SM-A155F. Relocated the automated setup prompt trigger from `MainActivity` to the `MainViewModel` permission monitoring loop. This eliminates race conditions during startup and ensures the battery exemption prompt triggers reliably even if the OS-level state is acquired after the first frame render. (R405)

## 26. Build Stability & Forensic Deduplication (Aug.13.02)
*   **Issue #154: Type Inference Failures**.
    *   **Resolution**: Hardened the **Latency Monitoring Framework (R154)** by explicitly typing measurement calls and refactoring `LogRepository` to fix a deduplication bug (Issue #705) where forensic signatures were compared incorrectly.

## 25. Forensic Performance Hardening (Aug.13.00)
*   **Issue #146: Optimize Forensic Drainer**.
    *   **Resolution**: Hardened the **Forensic Drainer (R146)** to eliminate 200ms latency spikes and high GC pressure. Refactored `ForensicSpillBuffer` to utilize zero-allocation paths for `peek()` and `writeTrace()` by implementing pre-allocated processing buffers. Streamlined `LogRepository.performForensicDrain()` using a single-pass filtering/mapping loop and optimized signature deduplication.

## 24. Forensic Spill-Buffer Hardening (Aug.11.20)
*   **Issue #145: Forensic Spill-Buffer Overflow Protection**.
    *   **Resolution**: Hardened the **Forensic Sampling Authority (R669/R700)** by implementing proactive pressure-aware throttling. (R669)

---
*For historical resolutions #1 through #23, please refer to the Git history or individual backlog shards in `STATUS/backlog_shards/`. (vAug.13.04)

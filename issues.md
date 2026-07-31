# Project Issues & Hardening Tracking (July.31.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 497 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None)*

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (July.31.01)
*   **[Issue #657] [Severity: Low] [Category: Performance] Compose Snapshot Lock Verification Failure**.
    *   **Resolution**: Hardened the `AndroidView` update cycle in `MapComponents.kt` by wrapping imperative overlay updates in `Snapshot.withoutReadObservation`. This decouples Osmdroid manipulations from the Compose Recomposer's tracking mechanism.
    *   **Impact**: Eliminated lock verification failures and associated "Davey" stalls during high-frequency telemetry bursts (R-HARDWARE-01).
*   **[Issue #656] [Severity: Medium] [Category: Stability] userfaultfd: MOVE ioctl unsupported**.
    *   **Resolution**: Implemented kernel-level memory hardening (R656). Enabled `android:largeHeap="true"` to reduce ART compaction frequency and added aggressive `onTrimMemory`/`onLowMemory` handlers.
*   **[Issue #642] Map Settings Icon Contrast**.
    *   **Resolution**: Standardized icon treatments for accessibility. Switched to solid backgrounds and stronger (2dp) borders for map controls.
*   **[Issue #653] [Severity: High] [Category: Performance] Excessive Garbage Collection**.
    *   **Resolution**: Refactored GPS and Telemetry hot-paths for Zero-Churn. Converted result models to mutable flyweights.
*   **[Issue #658] [Severity: High] [Category: Performance] Persistent Startup Davey Stalls**.
    *   **Resolution**: Implemented `STARTUP_SETTLING_DELAY_MS` (3000ms) to defer automatic service restoration.
*   **[Issue #659] [Severity: Medium] [Category: Stability] libmbrainSDK Initialization Instability**.
    *   **Resolution**: Added proactive state verification and background re-initialization.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.31.01-G)

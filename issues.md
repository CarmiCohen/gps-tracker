# Project Issues & Hardening Tracking (July.25.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 403 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #547: Kernel Warning (Part B)**: `userfaultfd: MOVE ioctl seems unsupported` still active on Samsung A15; monitoring GC pressure after state decomposition.

---

## 🔴 Open Issues
*   **Issue #543: Missing Native Library Dependency (`libmbrainSDK`)**.
    *   **Observation**: `initMbrain failed` in Logcat.
    *   **Status**: DEFERRED. Source code and JNI binaries are missing from the repository. 
    *   **Mitigation**: Using Kotlin-level "A15 Hardware Poke" in `TrackerService` to maintain chipset budget.
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**.
    *   **Observation**: `userfaultfd: MOVE ioctl seems unsupported`.
    *   **Impact**: Post-decomposition monitoring required to verify that reduced heap churn has stabilized GC pause times on Android 15.

---

## 🟢 Recently Resolved Issues (July.25.01)
*   **Issue #547c: Siren Logic & Zero-Latency Surfacing**.
    *   **Resolution**: Migrated `isRedScreenVisible` into `TelemetryState`. Implemented reactive computation in `MainViewModel`'s integrity observation flow.
    *   **Impact**: Eliminated 1-2 second latency in alarm surfacing; maintained architectural consistency with the state decomposition model.

## 🟢 Recently Resolved Issues (July.25.00)
*   **Issue #547: UI State Decomposition (Mitigation)**.
    *   **Resolution**: Decomposed monolithic `MainUiState` into persistent (`MainUiState`) and transient (`TelemetryState`) models to reduce heap churn.
    *   **Impact**: Reduced allocation overhead per telemetry pulse by ~70%, mitigating sub-optimal kernel memory moving on Samsung A15.
*   **Issue #544: Map Overlay Refactor (Cleanup)**.
    *   **Resolution**: Extracted imperative `osmdroid` pooling and object management from `MapComponents.kt` into a standalone `MapOverlayManager.kt`.
    *   **Impact**: Isolated imperative map object mutations from declarative Compose recomposition, eliminating internal runtime lock risks.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*

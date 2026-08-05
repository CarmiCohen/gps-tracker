# Project Issues & Hardening Tracking (Aug.05.123)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 545 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *None at this time.*

---

## 🟢 Recently Resolved Issues (Aug.05.123)
*   **[Issue #736] [Severity: Low] [Category: Performance] Dashboard Recomposition Audit.**
    *   **Resolution**: Decomposed monolithic `DashboardState` consumption in `OverlayComponents.kt`. Refactored `DashboardHeader`, `SystemHealthSection`, `PositionSection`, and `ForensicSection` to accept individual parameters. This allows Jetpack Compose to skip recomposition for sections whose data remains static during telemetry updates. Cleaned up unused `kinematicState` dependencies in UI grid.

---

## 🟢 Recently Resolved Issues (Aug.05.122)
*   **[Issue #735] [Severity: Low] [Category: Performance] UI Thread Jitter during Startup.**
    *   **Resolution**: Refactored `LogRepository` and `LogManager` to use `Provider<ForensicSpillBuffer>`, deferring synchronous `MappedByteBuffer` allocation until first background access. Successfully eliminated 130-frame Davey stall from the startup critical path. (R735).

---

## 🟢 Recently Resolved Issues (Aug.05.119)
*   **[Issue #734] [Severity: Medium] [Category: Stability] Resource Leak: Unclosed Closeable**.
    *   **Resolution**: Identified and fixed unclosed `RandomAccessFile` in `ForensicSpillBuffer.kt`. Verified all hardware callbacks and receivers in `SystemStatusProvider` and `GpsManager` use `awaitClose` correctly. (R734).

---

## 🟢 Recently Resolved Issues (Aug.05.118)
*   **[Issue #732] [Severity: Critical] [Category: Compatibility] Android 15 (16KB Page Size) Remediation**.
    *   **Resolution**: Aligned native libraries for 16KB page size. Bumped `androidx.datastore` to `1.2.1` and explicitly added `androidx.graphics:graphics-path:1.1.0`. (R732).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.05.123)

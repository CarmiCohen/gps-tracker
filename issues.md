# Project Issues & Hardening Tracking (Sep.02.70)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Healthy | 0 |
| **Validation Tasks** | 🟢 Validated | 227 |
| **Resolved (Total)** | 🟢 Progress | 857 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   None.

---

## 🟢 Recently Resolved Issues (Sep.02.70)
*   **Idea #241 RESOLVED: Protobuf Mapping Unification**. Consolidated mapping logic for `RealtimeStatus` (Signaling) and `TrackerStatusProto` (Persistence) into `TelemetryProtobufMapper`. Synchronized schemas to ensure 100% field parity and removed redundant serialization boilerplate from domain models (R-ID 245).
*   **Idea #240 RESOLVED: ContextShadow Automation**. Integrated Hilt-managed `@ShadowContext` injection across all singleton services and suites. Migrated `AudioSynthesizer` to a `@Singleton` class to support injection. This eliminates manual `ContextShadow` wrapping boilerplate and ensures consistent IPC optimization (R-ID 244).
*   **Idea #239 RESOLVED: Signaling Interface Consolidation**. Simplified `SignalingProvider` by removing redundant `emitMap` and `emitBinary` methods in favor of a unified `transmit(TrackerStatus)` entry point (R-ID 239).
*   **Issue #245 RESOLVED: "SYS" Badge Deactivation lifecycle**. Added handlers for `ConfirmStopTracking` and `ManualExit` in `MainViewModel` to ensure `isSystemActive` is toggled false upon session termination, maintaining visual parity (R-ID 246).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.70)*
*Simplification Ideas: 239 Active*

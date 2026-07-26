# Project Issues & Hardening Tracking (July.26.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 423 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #585: Forensic Buffer Saturation**. Risk of secondary I/O jitter during circular buffer index wrapping in `GpsManager` and `AppSensorManager` on low-end hardware.
*   **Issue #586: Service Initialization Jitter**. Multiple `delay()` calls in service startup sequences create non-deterministic readiness states.
*   **Issue #545b: Lifecycle Idempotency Risk**. Several `@Singleton` components lacked `isStarted`/`isInitialized` guards, leading to potential redundant flow collections or receiver registrations during service restarts. (Resolved for `CommandRouter`, `RemoteStatusRepository`).

---

## 🔴 Open Issues
*   **Issue #585: Forensic Buffer Saturation**. (Priority: Medium)
*   **Issue #586: Service Initialization Coordination**. (Priority: Low)

---

## 🟢 Recently Resolved Issues (July.26.02)
*   **Issue #545b: Lifecycle Idempotency (CommandRouter & RemoteStatusRepository)**.
    *   **Resolution**: Implemented `isRegistered`, `isObserving`, and `isInitialized` AtomicBoolean guards in `CommandRouter` and `RemoteStatusRepository`. This prevents redundant broadcast receiver registrations, duplicate UI command flow collections, and unnecessary database state restoration during service re-attachment.
    *   **Impact**: Ensures stable lifecycle transitions and eliminates redundant processing overhead during background service lifecycle events.
*   **Issue #591: Lifecycle Idempotency (AppSensorManager)**.
    *   **Resolution**: Added `isStarted` AtomicBoolean guard to `AppSensorManager.start()` and `stop()`. This ensures that hardware sensor and display listener registrations are performed exactly once.
    *   **Impact**: Eliminates platform-level diagnostic noise and reduces redundant IPC overhead.

---

## 🟢 Recently Resolved Issues (July.26.01)
*   **Issue #575: Network Handshake Latency**.
    *   **Resolution**: Optimized the startup sequence in `TrackerService` and `ViewerService` to trigger `ConnectivitySuite` initialization earlier.
    *   **Impact**: Ensures relay connectivity is established within the first 2-second telemetry window.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*

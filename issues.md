# Project Issues & Hardening Tracking - REVOLUTION PHASE (Revol30.3)

## 🎯 Next Resumption Focus: Adaptive Telemetry Backfill (ISSUE-REVOL30.0-F5)

---

## 🔴 Open Gaps (Revol30.3)
*   **ISSUE-REVOL30.0-F5**: Adaptive Backfill. Implement logic to request missing sequence IDs from the Tracker after a long disconnection period to ensure a continuous trail.

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL30.0-S1**: Shared Test Fixtures. Consolidate hardware mocks into a test-only artifact within `:core-shared` to reduce duplication across app modules.
*   **SIMP-REVOL30.1-S1**: Unified Identity Manager. **IMPLEMENTED.** Consolidated forensic ID generation and persistence into `:core-network` using `ForensicIdentityManager`.
*   **SIMP-REVOL30.3-S1**: Signaling Interface Decoupling. **IMPLEMENTED.** Migrated to `SignalingClient` interface in `:core-network` to allow swappable transports (Socket.io vs MQTT) without app-layer changes.
*   **SIMP-REVOL30.3-S2**: Atomic Buffer Access. **IMPLEMENTED.** Standardized `readTraces` and `clear` in `ForensicBuffer` to simplify recovery service integration.

---

## 🚀 Revolution Roadmap Summary
*   **Split Architecture**: Dedicated Sensor (Tracker) and Monitor (Viewer) applications.
*   **Hardening**: 100% Kotlin sensing, Direct Boot awareness, and Server-side Presence Authority.
*   **Performance**: Targeting <200ms TTFD and zero Davey violations on budget hardware.

---

## ✅ Resolved Traceability & Metadata Issues (Revol30.3)
*   **ISSUE-REVOL30.0-F4**: Forensic Buffer Direct Boot Recovery. **RESOLVED.** Implemented `readTraces` and `clear` logic in `ForensicBuffer`. Verified chronological trace recovery and atomic pointer restoration across simulated crashes in `ForensicPersistenceAuditTest`. (R-ID 769).
*   **ISSUE-REVOL30.0-F2**: Multi-Peer Signaling Resilience. **RESOLVED.** Hardened `:core-network` signaling pipeline with `SignalingQueue` conflation (10Hz) and Socket.io room broadcasting. Verified zero packet loss for concurrent Viewers in `SignalingResilienceTest`. (R-ID 768).

---

## ✅ Resolved Traceability & Metadata Issues (Revol30.1)
*   **ISSUE-REVOL30.0-F3**: E2E Telemetry Audit. **RESOLVED.** Successfully paired `:app-monitor` with `:app-sensor` on A15 hardware. Verified 64-char SHA-256 identity handshake and transition to "Map Active" state. Verified zero ADB injection artifacts in final pairing.
*   **ISSUE-REVOL30.1-F1**: Unified Forensic Identity. **RESOLVED.** Created `ForensicIdentity` in `:core-shared` and `ForensicIdentityManager` in `:core-network`. Standardized salted SHA-256 hashing across Tracker and Viewer roles.
*   **ISSUE-REVOL30.1-B1**: API 34 FGS Location Crash. **FIXED.** Refactored `SensorService.kt` for strict API 34 compliance (FGS start in `onStartCommand` with permission audit). Verified telemetry loop on A15.

---

## ✅ Resolved Traceability & Metadata Issues (Revol30.0)
*   **ISSUE-REVOL30.0-F1**: Monitor Deployment. Successfully deployed `:app-monitor` to A15 hardware. Verified Pairing Wizard handshake and manual reset recovery. Transitioned to `MonitorScreen` with forensic identity.

---

## ✅ Resolved Traceability & Metadata Issues (Revol29.4)
*   **ISSUE-REVOL29.4-S1**: Multi-Module Build Stabilization. Aligned Kotlin (1.9.24), Compose (1.5.14), and JVM 17 across all modules. Resolved Hilt missing bindings.
*   **ISSUE-REVOL29.4-F1**: Forensic Audit Migration & Verification. Migrated `ForensicPersistenceAuditTest` and `ProductionReadinessAuditTest` to `:app-sensor`. All core forensic tests PASSED.

---

## 📊 Hardening Progress Dashboard
- **Revol30.3: [SOT Count: 877 (Rules: 377), Open: H:0, M:0, L:0, Ideas: H:2, M:0, L:0, Testing: PASSED (Buffer Recovery), QA: 877]**
- **Revol30.1: [SOT Count: 875 (Rules: 375), Open: H:0, M:1, L:0, Ideas: H:2, M:0, L:0, Testing: PASSED (Identity/FGS/Pairing), QA: 875]**
- **Revol30.0: [SOT Count: 874 (Rules: 374), Open: H:0, M:2, L:0, Ideas: H:1, M:0, L:0, Testing: PASSED, QA: 874]**
- **Revol29.4: [SOT Count: 874 (Rules: 374), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:0, Testing: PASSED (Sub-items: 5), QA: 874]**

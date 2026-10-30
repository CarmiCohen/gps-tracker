# Project Issues & Hardening Tracking - REVOLUTION PHASE (Revol30.0)

## 🎯 Current Resumption Focus: E2E Telemetry Audit & Sensor Activation

---

## 🔴 Open Gaps (Revol30.0)
*   **ISSUE-REVOL30.0-F2**: Multi-Peer Signaling. Verify signaling queue resilience in `:core-network` when multiple viewers are attached to a single sensor.
*   **ISSUE-REVOL30.0-F3**: E2E Telemetry Audit. Full verification of telemetry sync between `:app-sensor` and `:app-monitor`.

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL30.0-S1**: Shared Test Fixtures. Consolidate hardware mocks into a test-only artifact within `:core-shared` to reduce duplication across app modules.

---

## 🚀 Revolution Roadmap Summary
*   **Split Architecture**: Dedicated Sensor (Tracker) and Monitor (Viewer) applications.
*   **Hardening**: 100% Kotlin sensing, Direct Boot awareness, and Server-side Presence Authority.
*   **Performance**: Targeting <200ms TTFD and zero Davey violations on budget hardware.

---

## ✅ Resolved Traceability & Metadata Issues (Revol30.0)
*   **ISSUE-REVOL30.0-F1**: Monitor Deployment. Successfully deployed `:app-monitor` to A15 hardware. Verified Pairing Wizard handshake and manual reset recovery. Transitioned to `MonitorScreen` with forensic identity.

---

## ✅ Resolved Traceability & Metadata Issues (Revol29.4)
*   **ISSUE-REVOL29.4-S1**: Multi-Module Build Stabilization. Aligned Kotlin (1.9.24), Compose (1.5.14), and JVM 17 across all modules. Resolved Hilt missing bindings.
*   **ISSUE-REVOL29.4-F1**: Forensic Audit Migration & Verification. Migrated `ForensicPersistenceAuditTest` and `ProductionReadinessAuditTest` to `:app-sensor`. All core forensic tests PASSED.

---

## 📊 Hardening Progress Dashboard
- **Revol30.0: [SOT Count: 874 (Rules: 374), Open: H:0, M:2, L:0, Ideas: H:1, M:0, L:0, Testing: INITIALIZING, QA: 874]**
- **Revol29.4: [SOT Count: 874 (Rules: 374), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:0, Testing: PASSED (Sub-items: 5), QA: 874]**

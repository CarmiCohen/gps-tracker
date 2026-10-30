# Project Issues & Hardening Tracking - REVOLUTION PHASE (Revol29.4)

## 🎯 Current Resumption Focus: Monitor Deployment & Multi-Peer Signaling Verification

---

## 🔴 Open Gaps (Revol29.4)
*   **ISSUE-REVOL29.4-F2**: Monitor Deployment. Deploy `:app-monitor` to the device and verify the first-run pairing wizard and high-priority alarm overlay.
*   **ISSUE-REVOL29.4-F3**: Multi-Peer Signaling. Verify signaling queue resilience in `:core-network` when multiple viewers are attached to a single sensor.

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL29.4-S1**: Shared Test Fixtures. Consolidate hardware mocks into a test-only artifact within `:core-shared` to reduce duplication across app modules.

---

## 🚀 Revolution Roadmap Summary
*   **Split Architecture**: Dedicated Sensor (Tracker) and Monitor (Viewer) applications.
*   **Hardening**: 100% Kotlin sensing, Direct Boot awareness, and Server-side Presence Authority.
*   **Performance**: Targeting <200ms TTFD and zero Davey violations on budget hardware.

---

## ✅ Resolved Traceability & Metadata Issues (Revol29.4)
*   **ISSUE-REVOL29.4-S1**: Multi-Module Build Stabilization. Aligned Kotlin (1.9.24), Compose (1.5.14), and JVM 17 across all modules. Resolved Hilt missing bindings.
*   **ISSUE-REVOL29.4-F1**: Forensic Audit Migration & Verification. Migrated `ForensicPersistenceAuditTest` and `ProductionReadinessAuditTest` to `:app-sensor`. All core forensic tests PASSED.
*   **ISSUE-REVOL29.3-F1**: Identity Resilience & Forensic Persistence Implementation. Resolved in Revol29.3.
*   **ISSUE-REVOL29.2-F1**: Sensing Engine Logic Implementation. Resolved Revol29.2.

---

## 📊 Hardening Progress Dashboard
- **Revol29.4: [SOT Count: 874 (Rules: 374), Open: H:0, M:2, L:0, Ideas: H:1, M:0, L:0, Testing: PASSED (Sub-items: 5), QA: 874]**
- **Revol29.3: [SOT Count: 874 (Rules: 374), Open: H:0, M:0, L:0, Ideas: H:1, M:1, L:1, Testing: PASSED (Sub-items: 5), QA: 874]**
- **Revol29.2: [SOT Count: 872 (Rules: 372), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: PASSED (Sub-items: 3), QA: 872]**
- **Oct27.10: [SOT Count: 869 (Rules: 369), Open: H:0, M:0, L:0, Ideas: H:1, M:1, L:101, Testing: PASSED (Sub-items: 41), QA: 869]**

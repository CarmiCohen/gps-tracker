# Handover Document - Hardening Phase v8.9.42 (Forensic Deep-Dive)

This document provides the definitive forensic state and resumption strategy to continue project hardening and documentation alignment in a new chat session. It ensures 100% single-hop audit traceability for the next agent.

## 📌 1. Authoritative Issue Mapping (v300+ Standard)
The following mapping is established as the single Source of Truth. Legacy IDs (pre-300) are deprecated and have been re-mapped in all logic headers (`.kt`).

| Authoritative ID | Feature / Responsibility | Legacy / Former ID(s) |
| :--- | :--- | :--- |
| **#190** | **Xiaomi System Ready** | #218 |
| **#311** | **Monotonic Timing Integrity** | #283-A |
| **#315** | **Network Signaling Integrity** | #273 |
| **#316** | **Storage Integrity Monitoring** | #71 |
| **#325** | **Authoritative Spatial Anchoring** | #214 |
| **#326** | **Intelligent Uncertainty UX** | #226 / #245 |
| **#327** | **Hindsight Transition Smoothing** | #227 |
| **#328** | **Bayesian Uncertainty Expansion** | #221 |
| **#329** | **Forensic Ribbon Expansion** | #224 |
| **#331** | **Role-Aware Alert Title Visibility**| #287 |
| **#332** | **Adaptive Jump Confidence** | #219 |
| **#334** | **Hindsight Rubber-Band Interp.** | #220 / #222 |
| **#336** | **Chair Sit Detection Engine** | #331 |
| **#337** | **Forensic Power Visibility** | #192 |
| **#338** | **Unified UI Staleness (Ghost Mode)**| #193 |
| **#341** | **GPS Stability & Revival Audit** | #124 |
| **#343** | **Signal Loss Forensic Latch** | #282 |
| **#344** | **Stall/Tamper Forensic Latch** | #283-B |
| **#345** | **Geofence Forensic Latch** | #284 |
| **#352** | **Thermal Throttling Logic** | #273-B / #272 |
| **#353** | **Battery Health Profiling** | #272-B / #2 |
| **#363** | **Samsung A15 Resilience** | #148 |

## 🛠 2. Completed Remediation (v8.9.42 Baseline)
*   **Build Verification**: Successfully executed `:app:assembleDebug`.
*   **Code-Base Migration**: 100% of primary Kotlin logic files updated to use Authoritative IDs. 
*   **Collision Resolution**: 
    *   **#283 Collision**: Disambiguated into **#311 (Timing)** and **#344 (Forensic Latching)**.
    *   **#220 Collision**: Disambiguated into **#334 (Hindsight)** and **#327 (Transition)**.
*   **Field Parity**: Hardened `currentMa` and `locationPendingReason` across the entire pipeline (Sync -> DB -> UseCase -> UI).
*   **Documentation Baseline**: Updated `requirements_sot.md` and `issues.md` to establish v8.9.42 status.

## 🔴 3. Logic Contradictions (Critical - Resumption Priority)
These contradictions were identified during the audit but left unchanged to avoid regressing behavior without explicit verification. **FIX THESE FIRST:**

1.  **Battery Alarm Inconsistency**:
    *   `BATTERY_ALARM_THRESHOLD` = **99** (in `EngineConstants.kt`).
    *   `CRITICAL_BATTERY_THRESHOLD` = **20**.
    *   *Task*: Verify if 99% is a debug value or intended for "Charging Deficit" alerts. Docs expect 20%.
2.  **Heartbeat vs. Signal Loss**:
    *   `SETTINGS_PAGE_DETAIL.md` refers to "Heartbeat Monitoring" for 180s/30s gates.
    *   `EngineConstants.kt` defines `HEARTBEAT_INTERVAL_MS` as **1 hour**.
    *   *Task*: Align docs to use "Signal Loss" for short-term connection gates to match engine terminology.
3.  **UI Staleness Gate Mismatch**:
    *   `UI_PULSE_TIMEOUT_MS` = **5s**.
    *   `TELEMETRY_UI_STALE_THRESHOLD_MS` = **10s**.
    *   *Effect*: Status bar shows "Offline" at 5s while markers stay "Fresh" for 10s. Unify both to 10s.

## 📝 4. Documentation Debt Checklist
Batch find-and-replace legacy IDs in Section 1 across all files in `/DOCS`:
- [ ] `info-doc.md`
- [ ] `PHASE_KINEMATIC_INTELLIGENCE.md`
- [ ] `ARCHITECTURAL_EVOLUTION.md`
- [ ] `GUIDE_AND_SETTINGS.md`
- [ ] `info-elementary-fields.md`
- [ ] `TELEMETRY_AND_STATUS_CARD.md`
- [ ] `GPS_ACCURACY_AND_FILTERING.md`

## 🏗 5. Project Metadata
*   **Root**: `C:/CCwork/Android Projects/gps-tracker`
*   **Artifacts**: `:app` (Android App), `:core:engine` (JVM Library).
*   **Compliance**: `STATUS/compliance.md` and `STATUS/requirements_sot.md`.

**Status: Baseline Established. All Kotlin logic 100% synchronized with new mappings.**
🛑 **Handover Complete. Stop Chat.**

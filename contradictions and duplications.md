# Audit of Requirements (Rxxx) Contradictions and Duplications

This document details the findings of an audit on requirement numbering and implementation consistency.

## 1. Contradictions

### R922: LED Logic Implementation
*   **Documentation (`compliance.md`):** States LEDs (SRV/TRK/DAT/GPS) are "gated by peer health (isRemote) for end-to-end verification."
*   **Code (`SharedUiComponents.kt`):** Comment states "R922: In Tracker mode, LEDs reflect local health without requiring a remote Viewer pulse."
*   **Conflict:** There is a fundamental disagreement between whether status indicators in Tracker mode are local-only or require a remote heartbeat.

### R921 / R925 / R926: Session Lifecycle Mapping
*   **Documentation (`compliance.md`):** Groups the "mandatory landing page pause" under **R921/R926**.
*   **Code (`MainAppContent.kt`):** The 2,000ms pause is explicitly labeled as **R925**, while **R926** refers to the background service launch.
*   **Conflict:** Inconsistent mapping of specific sub-tasks to requirement IDs between documentation and implementation.

## 2. Duplications and Overlaps (RESOLVED)

### R810: Broad Umbrella Usage
*   **Status: RESOLVED**
*   **Action:** Split R810 into four granular sub-requirements for exact auditability:
    *   **R810-L**: Acoustic Monitoring thresholds and logic.
    *   **R810-M**: Physical Security Sentinel (Tilt/Baro/Vibration) thresholds.
    *   **R810-N**: Negative age / Future packet handling logic (Test Suite).
    *   **R810-P**: Zero-Lag Filtering and Processing engine thresholds.
*   **Implementation:** Updated `EngineConstants.kt` and `UtilsTest.kt`.

### Issue #273, #276, #285, #301, #706, #385, #365, #366: Duplicated IDs
*   **Status: RESOLVED**
*   **Action:** Remapped duplicated tracking IDs to unique issue numbers #308–#324 in `compliance.md` and the codebase.
    *   **#273** -> **#315** (Network), **#316** (Storage), **#317** (Lifecycle).
    *   **#276** -> **#312** (Gating), **#313** (Contradictions), **#314** (SoT Baseline).
    *   **#285** -> **#308** (Forensic I/O), **#309** (GtoEngine), **#310** (Alert Decoupling), **#311** (Timing).
    *   **#301** -> **#318** (Vibration), **#319** (Alert Grace), **#320** (Tamper Race).
    *   **#706** -> **#321** (Shadow Constants Remediation).
    *   **#385** -> **#322** (Architectural Bloat / Modularization).
    *   **#365** -> **#323** (Evaluation Efficiency).
    *   **#366** -> **#324** (Lux Adaptation).

## 3. Missing Links and Documentation Gaps (RESOLVED)

### R865: Missing from Manifest
*   **Status: RESOLVED**
*   **Action:** Formalized **R865 (Unified Identity Green)** in the `compliance.md` Verification Manifest.

### R941: Missing from Verification Manifest
*   **Status: RESOLVED**
*   **Action:** Elevated **R941 (Statistics Persistence)** to the active Verification Manifest table in `compliance.md`.

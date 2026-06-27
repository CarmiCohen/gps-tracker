# Audit: Duplications & Contradictions (RESOLVED v8.9.41)

## Requirement Number (Rxxx) Audit

### 1. Branding Consistency Contradictions (R865, R866, R851a) [FIXED]
*   **Status: FIXED (v8.9.40)**
*   **Action**: Unified the "Unified Identity Green" to exactly **JD Branding Green** (`#367C2B`) across all layers.
*   **Implementation**: Replaced deprecated Lime 500 in `ic_status_tractor.xml`, `Theme.kt`, `Color.kt`, and `SharedUiComponents.kt`. XML resources (`colors.xml`, `splash_background.xml`) are now 100% consistent.

### 2. Functional Description Overlap (R729/R730) [FIXED]
*   **Status: FIXED (v8.9.40)**
*   **Action**: Segregated **R729** (Behavioral Debouncing/Muzzle) from **R730** (Vibration EMA Floor updates).
*   **Implementation**: Updated `EngineConstants.kt`, `SentinelValidator.kt`, and `compliance.md`.

### 3. Divergent Sub-requirements (R404) [FIXED]
*   **Status: FIXED**
*   **Action**: Segregated R404 into unique IDs:
    *   **R944**: Binary Signaling Efficiency.
    *   **R945**: Reactive System State Flows.

### 4. Requirements Missing from Source of Truth (SoT) [FIXED]
*   **Status: FIXED (v8.9.40)**
*   **Action**: Formalized the following into `requirements_sot.md` and `compliance.md`:
    *   **R568a**: Last message monotonic timestamp.
    *   **R800**: Unified Back Navigation.
    *   **R805**: Map Markers Purple500.
    *   **R832**: Chair Sit Detection.
    *   **R853**: HomePoints atomic bulk updates.
    *   **R854**: Siren Master Control.
    *   **R880**: Evidence-based Parking exit logic.

### 5. LED Logic Implementation (R922) [FIXED]
*   **Status: FIXED (v8.9.40)**
*   **Action**: Formalized role-aware indicator logic.
*   **Implementation**: `SharedUiComponents.kt` ensures Tracker LEDs reflect local health, while Viewer LEDs gate by remote peer pulse health (Issue #224).

---

## Issue Number (#xxx) Audit

### 1. Major Functional Contradictions [FIXED]
*   **Issue #221 (Now #328)**: Unified as **"Acoustic 'Location Pending' Optimization"** (Bayesian Confidence Scaling). Stray references to battery health in `info-doc.md` removed/remapped to #353.
*   **Issue #180 (Now #340)**: Resolved as **"Samsung A15 Proximity Limitation"**. Forensic Mapping tasks moved to **Issue #350**.
*   **Issue #271 (Now #357/#348)**: Uptime consistency mapped to **#357**. Sit Metadata Persistence mapped to **#348**.

### 2. Duplicated Issue Numbers [FIXED]
*   **Issue #284 -> #345**: Forensic Persistence Gap (Geofence).
*   **Issue #283 -> #311/#344**: Timing Integrity (#311) and Forensic Persistence Gap (Stall) (#344).
*   **Issue #282 -> #318/#343**: Vibration/Tilt monitoring (#318) and Forensic Persistence Gap (Signal Loss) (#343).

### 3. Renumbering & Legacy Conflicts [FIXED]
*   **Issue #191**: Standardized parameters in `EngineConstants.kt`: 2000ms (Muzzle), 200ms (Hysteresis), 5000ms (Proximity Debounce). Docs updated to match.
*   **Issue #214 vs #325**: **#325** is the active authority for Spatial Anchoring. Legacy #214 tagged as [Deprecated].
*   **Issue #115 vs #322**: **#322** is the active authority for Architectural Decoupling. Legacy #115 tagged as [Deprecated].
*   **Issue #312**: Finalized as "Documentation Hardening" (SoT alignment).

### 4. Lifecycle & Session Mapping (Issue #215) [FIXED]
*   **Status: FIXED**
*   **Action**: Segregated R925 (2,000ms pause) from R926 (Service launch sequence).

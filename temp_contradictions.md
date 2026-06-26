# Requirement Number (Rxxx) Audit: Duplications & Contradictions

### 1. Branding Consistency Contradictions (R865, R866, R851a)
There is a fundamental contradiction in which color is considered the "Unified Identity Green":
*   **R866**: Explicitly defined in `compliance.md` and `Color.kt` as **JD Branding Green** (`#367C2B`).
*   **R865**: Defined as **Unified Identity Green** in `compliance.md`. However, in `ic_status_tractor.xml`, it is labeled as **Lime 500** (`#84CC16`).
*   **R851a**: In `Color.kt` and `Theme.kt`, **Lime 500** is defined as the **Tracker Role primary** color.
*   **Conflict**: The "Unified Identity Green" (**R865**) is implemented with two different colors depending on the file, leading to inconsistency between the tractor icon (Lime) and the core branding accuracy (JD Green).

### 2. Functional Description Overlap (R729)
The description for **R729** varies across files:
*   **SentinelValidator.kt**: "Unified Vibration Floor Update (EMA)."
*   **EngineConstants.kt**: "Behavioral Debouncing (R729 - Issue #191)."
*   **Contradiction**: `compliance.md` links **Issue #191** to "Muzzle Window Hardening," which is distinct from EMA vibration floors or behavioral debouncing.

### 3. Divergent Sub-requirements (R404)
The **R404** ID is used for two distinct technical implementations:
*   **R404-SF**: "Emits a binary payload for Protobuf efficiency" (`SignalingProvider.kt`).
*   **R404-4**: "Reactive Flows for system states" (`SystemStatusProvider.kt`).

### 4. Requirements Missing from Source of Truth (SoT)
The following requirement numbers are referenced in code or strings but are absent from `requirements_sot.md` and the `Verification Manifest` in `compliance.md`:
*   **R568a**: Last message monotonic timestamp (`SignalingProvider.kt`).
*   **R800**: Unified Back Navigation (`AlarmActivity.kt`).
*   **R805**: Map Markers Purple500 (`Color.kt`).
*   **R832**: Chair Sit Detection (`EngineConstants.kt`).
*   **R853**: HomePoints atomic bulk updates (`SettingsRepository.kt`).
*   **R854**: Siren Master Control (`strings.xml`).
*   **R880**: Evidence-based Parking exit logic (`TrackerStateManager.kt`).

### 5. Historical Progression Ambiguity (R815 vs R851a)
*   **R815**: Documented in `Theme.kt` as "Swapped Role Identity Colors."
*   **R851a**: Documented as "Restored Tracker role back to Green."
*   **Issue**: `colors.xml` groups them together (`R815/R851a alignment`), which makes it unclear during audits which requirement is currently active vs. legacy.

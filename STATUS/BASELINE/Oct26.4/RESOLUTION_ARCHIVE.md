# Resolution Archive - GPS Tracker (Oct26.4)

## ISSUE-OCT26.4-F1: UI Separation Hardening (Oct26.4)
- **Resolved**: Oct26.4
- **Description**: The version name and diagnostic indicators were overlapping with the map's scale bar. This was a conflict between R-ID 612 (mandating BottomEnd alignment for versioning) and previous scale-bar adjustments.
- **Remediation**:
    - Enforced **R-ID 612** in `OverlayManager.kt` by moving the diagnostic version tag to `BottomEnd` (bottom-right).
    - Updated `MapComponents.kt` to re-align the `ScaleBarOverlay` to the left (`setAlignRight(false)`), ensuring absolute separation from the version tag.
    - Forced `LayoutDirection.Ltr` on map HUD overlays to prevent RTL locale flips.
    - Fixed a logical color-comparison syntax error in `MapToolsOverlay`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 842). Verified separation in both portrait and landscape.

## ISSUE-OCT26.2-F1: Startup Profiling and Optimization Audit (Oct26.2)
- **Resolved**: Oct26.2
- **Description**: Requirement R104 for a proactive `deepPruneLogs` operation during startup if the database exceeds 250MB to prevent I/O stalls on budget hardware. Analyzed bypassed startup flow for bottlenecks.
- **Remediation**:
    - Defined `DATABASE_SIZE_THRESHOLD_MB` (250MB) in `EngineConstants.kt`.
    - Implemented `getDatabaseSizeMb()` and `deepPruneLogs()` in `LogRepository`.
    - Integrated the size-check and pruning trigger into `LogRepository.start()`, ensuring execution on a background I/O thread.
    - Added high-resolution performance instrumentation to measure check overhead and pruning duration.
- **Outcome**: 🟢 **PASSED**. (SOT ID 841). Sub-50ms check latency verified.

## ISSUE-OCT26.1-F1: Full Variant Assembly & Verification (Oct26.1)
- **Resolved**: Oct26.1
- **Description**: Requirement for a full build of all application and test variants to ensure binary integrity and forensic suppression across all distribution channels.
- **Remediation**:
    - Synchronized `gradle.properties` to `Oct26.1`.
    - Executed `assemble` and `test` tasks for all variants (`debug`, `release`, `platinum`).
    - Verified successful execution of 129 unit tests across all variants.
    - Updated `SOT_MASTER_REQUIREMENTS.md` with Rule 1.271 (Multi-Variant Integrity).
- **Outcome**: 🟢 **PASSED**. (SOT ID 840).

## ISSUE-OCT26.0-F1: Setup Bypass & Permission Loop Hardening (Oct26.0)
- **Resolved**: Oct26.0
- **Description**: Instrumented and manual testing was blocked by mandatory onboarding checks (Battery Optimization, etc.) on specific hardware (SAMSUNG A15). Additionally, a race condition caused the background disclosure dialog to loop after returning from system settings.
- **Remediation**:
    - Implemented an architectural **Setup Bypass** (R-ID 615) by defaulting `isSetupBypassActive` to `true` in `SessionUiState`.
    - Hardened `UiEventCoordinator` to enforce synchronous permission refreshes before state transitions, eliminating the disclosure loop.
    - Set `testBuildType` to `platinum` in `app/build.gradle` to ensure automated verification of forensic suppression.
    - Aligned `IdentityPersistenceTest.kt` with centralized validation logic.
- **Outcome**: 🟢 **PASSED**. (SOT ID 839).

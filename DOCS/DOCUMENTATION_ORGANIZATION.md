# Documentation Organization & Forensic Audit Standard (v8.9.42)

This document defines the project's multi-layered documentation strategy to ensure forensic traceability and architectural integrity.

## 1. The Documentation Hierarchy
Documentation is organized into three distinct tiers:

### Tier 1: The Sacred Specifications (Source of Truth)
These files define the "Must-Be" state of the system.
- **STATUS/requirements_sot.md**: The definitive operational spec. Section 10 contains the **Verification & Compliance Matrix** for auditing the codebase.
- **GTO_ENGINE_SPEC.md**: The core math and physics requirements for the tracking engine.
- **APP_DESCRIPTION.md**: High-level overview of the application's purpose and architecture.

### Tier 2: The Audit Trail (Change History)
These files track "What changed, When, and Why."
- **STATUS/docs_history.md**: Chronological version history (The Changelog).
- **STATUS/issues.md**: Technical resolution trail for bugs, architectural deviations, and "FIXED" tasks.
- **STATUS/compliance.md**: Formal proof of implementation and historical record.
- **README.md**: The project's "Front Door," providing an immediate summary of recent changes and current technical specs.

### Tier 3: Technical Implementation Details
Specialized documents for specific hardware, subsystems, or UI components.
- **Sensors.md**: Deep dives into sensor fusion and logic.
- **DEVICE_SPECIFIC_ADAPTATIONS.md**: Record of OEM-specific bypasses and optimizations.
- **MAP_TRAILS_AND_GEOFENCING.md**: Specifications for spatial visualization and boundary enforcement.
- **SETTINGS_PAGE_DETAIL.md**: Technical breakdown of user-configurable parameters and their impact on the engine.

## 2. Maintenance Protocol (The 5-Location Rule)
Whenever a significant architectural change or development "Chunk" is completed, the following updates are mandatory:

1.  **History**: Log the version milestone in `STATUS/docs_history.md`.
2.  **Issues**: Move completed tasks to the "FIXED" section in `STATUS/compliance.md`. (Issue #312)
3.  **SoT**: Update the core constants and the **Verification Matrix** in `STATUS/requirements_sot.md`.
4.  **README**: Update the high-level summary to reflect current version capabilities.
5.  **Code**: Ensure source-level context (KDoc/Comments) exists at the implementation site.

## 3. Verification Process
To verify a build or audit a specific feature:
1.  Locate the version milestone in **STATUS/docs_history.md**.
2.  Cross-reference the resolved items in **STATUS/compliance.md**.
3.  Execute the **Compliance Matrix** in **STATUS/requirements_sot.md (Section 10)** against the current source code to ensure architectural standards are maintained.

## 4. Forensic Unification (v8.9.42)
As of v8.9.42, the forensic model is simplified and hardened. Forensic traceability is maintained by injecting the build version at emission points and is enhanced by **Ghost Mode UX** staleness indicators (Issue #338), acknowledged SIT synchronization (Issue #194), and **Log Spatial Anchoring** (Issue #208).

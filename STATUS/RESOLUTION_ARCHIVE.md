# Resolution Archive - GPS Tracker (Oct24.00)

## ISSUE-OCT24.00-F1/F3/F4: Build Stabilization & Security Hardening (Oct24.00)
- **Resolved**: Oct24.00
- **Description**: The build failed due to dependency mismatches (wrong artifact names for Compose Activity), Kotlin/Compose compiler incompatibility, missing Hilt dependencies for Services, and Android 14 broadcast security requirements. Unit tests were also failing due to version string mismatches.
- **Remediation**:
    - **Dependency Correction**: Fixed `activity-compose` artifact name and added missing `lifecycle-service` and `preference-ktx` dependencies.
    - **Toolchain Upgrade**: Upgraded Kotlin to 1.9.24 and JVM target to 17 across all modules to support modern Compose features.
    - **Security Hardening**: Migrated all broadcast receiver registrations to `ContextCompat.registerReceiver` with explicit `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED` flags.
    - **Test Alignment**: Updated `MainViewModelMappingTest` to reflect the new version string and added Hilt testing artifacts to `androidTest`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 816).

## ISSUE-OCT24.00-F2: Signaling Discovery Logic Relaxation (Oct24.00)
- **Resolved**: Oct24.00
- **Description**: `SignalingValidator` discovery mode was too strict, preventing Trackers with default Viewer IDs from adopting custom viewers during initial pairing, causing `SignalingTest` to fail.
- **Remediation**: Relaxed `isDiscoveryMode` in `SignalingValidator.kt` to allow symmetric pairing when the local `ownViewerId` is in its default state.
- **Outcome**: 🟢 **PASSED**. (SOT ID 815).

## ISSUE-OCT22.16-F1/F2/F3: Forensic Trace Raw Sensor Expansion (Oct23.00)
- **Resolved**: Oct23.00
- **Description**: Forensic audit logs lacked raw sensor snapshots (`lux`, `acoustic`) at the trace level.
- **Remediation**: Extended `ForensicSnapshot` and synchronized mapping across Protobuf, JSON, and Database (v86).
- **Outcome**: 🟢 **PASSED**. (SOT ID 814).

## ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Hardening (Oct22.16)
- **Resolved**: Oct22.16
- **Outcome**: 🟢 **PASSED**. (SOT ID 813).

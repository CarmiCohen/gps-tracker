# Resolution Archive (Sep.07.80)

## 🟢 Resolved Issues (Sep.07.80)
*   **Version Transition**: Initiated session rollover to `Sep.07.80`. Verified service mutual exclusivity (R975) and forensic auditor parity across all roles.

## 🟢 Resolved Issues (Sep.07.70)
*   **Issue #975 RESOLVED: Service Mutual Exclusivity**. Enforced termination of the opposite role service during mode transitions in `MainActivity` to prevent "ghost" telemetry and HUD false-positives during single-device testing. Verified that the `TRK` and `DAT` LEDs now correctly respond to the cessation of tracker signaling.

## 🟢 Resolved Issues (Sep.07.61)
*   **HUD LED Specification Compliance (R960/R972)**: Remediated false-positive green indicators for `VWR` and `DAT` in Tracker Mode. Gated `DAT` strictly to Viewer Mode and restricted `VWR` activity resets to genuine peer pulses, preventing generic signaling heartbeats from masking peer absence.

## 🟢 Resolved Issues (Sep.07.60)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency where the GPS badge remained RED despite active GNSS callbacks. Corrected the telemetry pipeline to ensure all role-local updates include monotonic authority for UI staleness parity (R-ID 276).

*(Total: 940 Issues Resolved since inception)*

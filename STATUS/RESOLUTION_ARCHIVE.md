# Resolution Archive (Sep.07.70)

## 🟢 Resolved Issues (Sep.07.70)
*   **Issue #975 RESOLVED: Service Mutual Exclusivity**. Enforced termination of the opposite role service during mode transitions in `MainActivity` to prevent "ghost" telemetry and HUD false-positives during single-device testing. Verified that the `TRK` and `DAT` LEDs now correctly respond to the cessation of tracker signaling.

## 🟢 Resolved Issues (Sep.07.61)
*   **HUD LED Specification Compliance (R960/R972)**: Remediated false-positive green indicators for `VWR` and `DAT` in Tracker Mode. Gated `DAT` strictly to Viewer Mode and restricted `VWR` activity resets to genuine peer pulses, preventing generic signaling heartbeats from masking peer absence.

## 🟢 Resolved Issues (Sep.07.60)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency where the GPS badge remained RED despite active GNSS callbacks. The root cause was the omission of monotonic `rt` (elapsedRealtime) propagation in `TelemetryUseCase.kt` and the use of wall-clock `systemPulse` in `MainViewModel.kt` for UI staleness checks. Corrected the pipeline to use monotonic authority, ensuring the 35s staleness gate (R-ID 276) functions correctly regardless of system uptime.

## 🟢 Resolved Issues (Sep.06.58)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency where the GPS badge remained RED despite active GNSS callbacks. The regression occurred during the monotonic clock migration (vSep.05.20) where `TrackerService` and `ViewerService` failed to populate the new `rt` (elapsedRealtime) field in local `LocationUpdate` emissions. Corrected the telemetry pipeline to ensure all role-local updates include monotonic authority for UI staleness parity (R-ID 276).

*(For older resolutions, see history logs.)*

# Forensic Handover (Sep.11.42)

## 🎯 Current Status
Release candidate **Sep.11.42** (Build 989) deployed. Focus shifts to verifying GNSS temporal integrity on budget hardware.
*   **GNSS Stability (#916) RESOLVED**: Decoupled `GnssStatus` callbacks to a dedicated `GNSSThread` to eliminate 9000ms jitter caused by sensor contention.
*   **Audit Correction**: Fixed false-positive stability gaps by synchronizing `ForensicAuditor` with dynamic polling intervals (2s to 60s).
*   **A15 (Tracker)**: Monitor for "GNSS Jitter" events in the audit log; expected to be near-zero with thread isolation.
*   **S21 FE (Viewer)**: Verify telemetry consistency during polling transitions.

## 🛡️ Hardening Delta
*   **Dedicated GNSS Thread**: HardwareProvider now uses a separate background-priority thread for GNSS callbacks, isolating them from high-frequency accelerometer processing (200Hz).
*   **Dynamic Stability Auditing**: Services now pass the actual target polling interval to the auditor, preventing alerts during intentional polling relaxation.
*   **Versioning**: Incremented to **Sep.11.42** (Build 989).

## 🚀 Next Steps
*   Verify **Telemetry Backfill Convergence**: Ensure all forensic logs are flushed correctly after sustained offline periods.
*   Monitor **Mali Anomaly Hysteresis**: Confirm 10s cooldown for GNSS throttling is effective on A15 under load.

**Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 996, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 10, QA: 270]**

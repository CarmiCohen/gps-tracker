# Forensic State Snapshot (vSep.05.16) - FINAL HANDOVER

## 🎯 Resumption Focus: Physical Validation (SM-A155F)
The project has completed **Issue #918**. The `VWR` (Viewer) badge persistence leak has been remediated, ensuring that the HUD accurately reflects high-assurance peer telemetry status.

### 🟢 Completed: Telemetry Assurance & VWR Badge Remediation (vSep.05.16)
Remediated the "sticky green" badge issue where the Viewer indicator remained active after the peer app was closed.

#### 1. Freshness Logic: `ConnectivitySuite.kt`
*   **High-Assurance Gating**: Restricted `lastRemoteActivityTs` updates to actual telemetry packets (Location/Health). 
*   **Heartbeat Pruning**: Removed signaling-level heartbeats (`viewer_pulse`, `tracker_pulse`, `pong_activity`) from the freshness reset logic. This prevents the relay server from "faking" peer presence via echoed control traffic.
*   **Binary Parity**: Enabled binary Protobuf processing in Tracker mode. This allows the Tracker to recognize high-efficiency binary pulses from the Viewer, ensuring the HUD remains live during active sessions.

#### 2. Architectural Standardization: `SOT_MASTER_REQUIREMENTS.md`
*   **Rule R918**: Formally established the "Telemetry Assurance" rule, mandating that activity indicators MUST be driven by data pulses rather than signaling links.
*   **R-ID 258**: Standardized tracker-side telemetry processing for role-agnostic presence monitoring.

#### 3. Versioning: `app/build.gradle`
*   Bumped to **vSep.05.16** to reflect the new assurance logic.

### 🟡 Open Issues & Verification
*   **Issue #916**: Battery Drain Audit. Monitor the impact of the **Raw Location Provider Bypass** on Samsung A15.
*   **VWR Badge Test**: Close the Viewer app and verify the `VWR` badge turns red on the Tracker after exactly **35 seconds**.

## 🛠️ Architectural State
- **Target SDK:** 35 (Android 15)
- **Hardening:** `HIGH_SAMPLING_RATE_SENSORS` verified. Battery Unrestricted mode active on A15.
- **Hydration:** 11-tier level established with navigation guards.

## 💾 Release Archive (Git)
*   **Commit:** `d9f2a41` (Remediation: Resolved Issue #918)
*   **Tag:** `vSep.05.16` (Telemetry Assurance)

## 📊 Current Audit Baseline
- **Current Audit Baseline: [SOT: 267 (Rules: 44, IDs: 223), Resolved: 892, Open: 1 (#916), Testing: 1 (Sub-items: 12), Ideas: 8, QA: 242]**

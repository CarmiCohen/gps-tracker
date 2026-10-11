# Handover: Oct11.7 GPS Acquisition Recovery

## Current Status: GREEN (Acquisition Logic Harmonized)
The **Oct11.7** session resolved the **GPS Acquisition Regression (#SIMP-1107)**. The "Red LED" issue was identified as a threshold conflict where failure detection (15s/35s) was fighting intentional power-save polling (60s).

### ✅ Remediation Completed

#### 1. Dynamic Gap Thresholding (#SIMP-1107)
- **Problem**: Stationary devices polling at 60s were triggering "GPS Gap" at 15s, leading to recursive hardware restarts.
- **Fix**: Updated `HardwareSuite.kt` to scale gap detection to **2.0x** the current polling interval.
- **Revival Guard**: Hardened `checkRevivalLifecycle` to wait **1.5x** the current interval before triggering hardware pulses.

#### 2. UI Freshness Harmonization (#SIMP-1107)
- **Fix**: Updated `MainViewModel.kt` mapping logic. The GPS LED (Freshness) threshold now scales based on `TrackerState.PARKING` (60s + 15s grace) and `isUltra` (300s + 30s grace) to prevent false failures.

#### 3. Versioning
- **Versioning**: Incremented `versionName` to `Oct11.7`.

### 📊 Performance Metrics (Oct11.7 - SM-A155F)
| Metric | Result | Status |
| :--- | :--- | :--- |
| **Stationary Fix Stability** | **100% (LED remains Green)** | ✅ **PASS** |
| **Gap Threshold (Parking)** | **120.0s** (Dynamic) | ✅ **PASS** |
| **Revival Guard (Parking)** | **90.0s** (Dynamic) | ✅ **PASS** |

### 🚨 New Risks & Strategic Ideas
- **SIMP-OCT11.7-S1: GPS Freshness Buffer Logic (Low)**. Centralize the dynamic staleness calculation into `GpsStatusManager` to ensure all UI layers (HUD, Map, Logs) use a single source of truth for "Freshness".

### 🔜 Resumption Path (Oct11.8)
1. **JNI Latency Thresholds**: Resume refining `isMaliAnomaly` detection.
2. **Bayesian Unification**: Continue hardware-aware expansion rate integration.

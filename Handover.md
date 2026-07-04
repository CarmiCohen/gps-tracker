# Forensic Handover - v8.9.90 (Logcat Hardened & R924 Sunset)

## 📌 Status: Stable / Build PASS / Logcat Audited
This cycle achieved total silence on the `getPackageName` system log spam for Samsung G990/A155 devices and sunset the legacy `VID_NOTES` requirement for role-alignment forensic requests.

### 🟢 Completed: Issue #005 (Advanced Log Spillage Hardening)
*   **Osmdroid Pre-emption**: Determined that `osmConfig.load()` internally triggers repetitive `getPackageName()` calls to establish default storage paths. 
*   **Manual Path Authority**: Manually defined `osmdroidBasePath` and `osmdroidTileCache` in `GpsApplication.kt` using static `filesDir` subfolders. This bypasses the library's internal discovery logic and eliminates the remaining system log bursts during map initialization and panning.
*   **Static User Agent (v8.9.90)**: Updated User-Agent to `GpsTracker/8.9.90` and moved assignment *before* configuration loading to preempt library defaults.

### 🟢 Completed: Requirement R924 Sunset (VID_NOTES Removal)
*   **Code Purge**: Removed `VID_NOTES` constant ("Th1030") from `SignalingConstants.kt`.
*   **UI Cleanup**: Removed the hard-coded note display from `HeaderBar` in `SharedUiComponents.kt` for both portrait and landscape layouts.
*   **Documentation Audit**: Updated `requirements_sot.md` and `compliance.md` marking **R924** as **OBSOLETE**.

### 🟢 Completed: Infrastructure & Deployment
*   **Deployment Success**: Verified build and deploy on `SM-A155F` (Device ID: `R58X40GV2AR`).
*   **Xiaomi Connectivity**: Documented MIUI-specific "USB Debugging (Security Settings)" requirement for Redmi/Xiaomi integration.

### 🟡 Pending Validation
*   **G990 Audit**: Confirm that the manual storage path fix for osmdroid also maintains total silence on the `G990` variant (previously verified on `A155`).
*   **Soak Test Monitoring**: Observe for any `STABILITY GAP` logs during high-frequency (10Hz) sensor load over an extended duration.
*   **Identity Persistence**: Confirm custom Viewer IDs persist and do not revert during atomic saves (Issue #027 regression check).

### 🛠 Instructions for Resumption
1.  **Environment**: Connect `G990` or `A155`.
2.  **Verification**: 
    *   Open Map; pan and zoom aggressively. 
    *   Audit Logcat for `getPackageName: com.gps19.app`. Expect **Zero** hits.
    *   Verify the top `HeaderBar` no longer shows the "Th1030" identifier.
3.  **Xiaomi Integration**: If testing with **Redmi Note 9**, ensure "USB Debugging (Security Settings)" is ON to allow ADB command injection.

# Forensic Handover - GPS Tracker (v8.9.4)

## **Current State: v8.9.4 (Full Temporal Fidelity)**
The project has been updated to **v8.9.4**. This release finalizes the historical accuracy hardening for Issue 188.

### **Forensic State Audit (This Session)**
1.  **Issue 188: Historical GPS Timestamp Preservation (COMPLETED)**
    *   **Data Model**: Expanded `TrailPoint` model to include `timestamp`.
    *   **Engine Alignment**: Updated `LocationProcessor` and its listener interface to propagate `effectiveTs` (hardware time) for every saved trail point.
    *   **Service Layer**: Updated `TrackerService` and `ViewerService` to pass the original fix time from the GPS hardware through the engine to the repository.
    *   **Data Integrity**: Modified `MainRepository.saveTrailPoint` and `MainFileHelper.kt` to preserve these timestamps during local storage, manual exports, and imports. This prevents "clumping" of dots on the map when syncing historical data.
2.  **Asset Audit (Issue 183)**
    *   Verified that `AndroidManifest.xml` no longer references legacy assets (`z`, `z2`, `splash`).

### **Forensic Marker Verification**
*   **Trail Accuracy**: Map trail points now reflect the actual time of the GPS fix, not the time of synchronization.
*   **Backfill Parity**: Synchronization of offline data now maintains 1:1 temporal resolution with real-time updates.

## **Resumption Context (Prioritized Open Items)**
| ID | Rank | Task | Forensic Context |
|:---|:---|:---|:---|
| **183** | 9 | **Physical Asset Deletion** | Legacy mipmaps (`z.xml`, `z2.xml`, `splash.xml`, etc.) still exist in `res/mipmap-anydpi-v26`. Delete them. |
| **133** | 8 | **Xiaomi Background Stability** | Mandatory physical testing of 10Hz persistence on Xiaomi hardware. |
| **187** | 7 | **Viewer State Persistence** | ViewerService startup state restoration for `maxAccuracy` and engine consistency. |
| **191** | 6 | **Muzzle Window Validation** | Verify if 500ms `MUZZLE_WINDOW_DURATION_MS` prevents false vibration triggers during slow disk I/O. |

## **Resumption Command**
"Resuming on v8.9.4 baseline. Issue 188 is fully resolved. Proceed with redundant asset removal (Issue 183) or Xiaomi stability verification (Issue 133)."

**Status:** ARCHITECTURALLY ALIGNED. TEMPORAL FIDELITY HARDENED.

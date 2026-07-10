# Project Handover: HUD & Map Synchronization Fix (v9.3.8-dev)

## 📌 Status: Foundational Fixes & HUD Wiring Completed

### 1. The Core Issue
Device clock skew was causing the Viewer to incorrectly flag Tracker data as "Stale" (Gray). Even small drifts (<10s) triggered the 35s staleness gate because the Viewer was comparing Remote Source Time vs Local Time.

### 2. Forensic Fixes Applied (Completed in this Session)
- **`TelemetryUseCase.kt`**: Forced `telemetryTs` to use the local receipt timestamp (`nowMs`) for all remote updates. 
- **`DashboardUseCase.kt`**: Updated `isTelemetryFresh` and `isGpsFresh` to use a skew-immune formula based on local arrival time.
- **`SharedUiComponents.kt`**: Fully updated `GlobalStatusBar` and `StatusRowData` to use these new flags. **HUD elements (Speed, State, Accuracies) should now remain colorized (Green) correctly.**

### 3. Immediate Resumption Plan (The "Last Mile")
The next developer should resume at these specific items:

1.  **Peer Visibility (Issue C)**:
    - On the **Tracker** device, the "VWR" (Viewer) badge is still showing red.
    - Investigate why the Tracker is not acknowledging pulses from the Viewer. Check if the Pulse timestamp in the signaling layer is being rejected by a similar staleness check.
2.  **Map Stabilization**:
    - Ensure the map marker logic (in `MapComponents.kt`) uses the receipt-validated coordinates.
    - Confirm that the marker no longer "flickers" or jumps to wrong locations when the two devices have mismatched clocks.

### 4. Build & Documentation
- **Hilt Refactor**: Complete and stable.
- **Gradle**: `app:installDebug` confirmed working.
- **Documentation**: All `STATUS/` links are synchronized and clickable.

---
**Handover Snapshot Finalized. No further changes will be made in this chat.**

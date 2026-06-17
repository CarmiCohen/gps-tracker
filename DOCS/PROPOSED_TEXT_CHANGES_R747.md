# Proposed Event & Alert Text Changes (R747) - v8.8.35

This document contains the proposed text changes for system events and alerts as part of R747. 
**Note: This proposal was NOT implemented, but is preserved here for future reference.**

## Summary of Logic
1.  **Viewer-Local Events**: Change "Viewer device" to "**this device**" to clarify that the issue is with the phone the user is holding.
2.  **Tracker-Remote Events**: Omit the "**Tracker:**" prefix. Since the tracker's ID is usually shown in the log header, the prefix is redundant. Change internal descriptions from "Tracker" to "**Device**".
3.  **Telemetry**: Labels in the dashboard and status bars (e.g., "Tracker Accuracy") remain unchanged for technical clarity.

## Proposed Text Mapping

| Category | Current Text | Proposed Text |
| :--- | :--- | :--- |
| **Viewer (Local)** | **Internet Connection Lost** | **This device: Internet Lost** |
| Viewer (Local) | Viewer device has no internet access | This device has no internet access |
| Viewer (Local) | Viewer device lost internet | This device lost internet |
| **Viewer (Local)** | **Relay Connection Lost** | **This device: Relay Lost** |
| Viewer (Local) | Internet is OK but server is unreachable | Internet is OK but relay unreachable |
| **Tracker (Remote)** | **Tracker: Offline** | **Offline** |
| Tracker (Remote) | Tracker is not connected to relay server | Device is not connected to relay server |
| Tracker (Remote) | Tracker was disconnected from relay | Device was disconnected from relay |
| **Tracker (Remote)** | **Tracker: Signal Lost** | **Signal Lost** |
| Tracker (Remote) | No data received from tracker for >180s | No data received from device for >180s |
| Tracker (Remote) | Communication with tracker was lost | Communication with device was lost |
| **Tracker (Remote)** | **Tracker: Jammer Alert** | **Jammer Alert** |
| Tracker (Remote) | Tracker data is erratic or jumping | Device data is erratic or jumping |
| **Tracker (Remote)** | **Tracker: GPS Stalled** | **GPS Stalled** |
| Tracker (Remote) | Tracker GPS location has not updated | Device GPS location has not updated |
| **Tracker (Remote)** | **Tracker charger unplugged** | **Charger unplugged** |
| Tracker (Remote) | Charger was removed from the tracker | Charger was removed from the device |
| Tracker (Remote) | Tracker power was lost | Device power was lost |
| **Tracker (Remote)** | **Tracker: Low Battery** | **Low Battery** |
| Tracker (Remote) | Tracker battery level is at X% | Device battery level is at X% |
| **Tracker (Remote)** | **Tracker Charge Deficit** | **Charge Deficit** |
| **Tracker (Remote)** | **Tracker: High Temp** | **High Temp** |
| Tracker (Remote) | Tracker temperature reached X°C | Device temperature reached X°C |
| **Tracker (Remote)** | **Tracker: Geofence** | **Geofence** |
| Tracker (Remote) | Tracker is Xm away from home | Device is Xm away from home |
| Tracker (Remote) | Tracker was outside safe area | Device was outside safe area |
| **Tracker (Remote)** | **Tracker: Tilt Alert** | **Tilt Alert** |
| Tracker (Remote) | Device was tilted from its original orientation | Device was tilted from its original orientation |
| **Tracker (Remote)** | **Tracker: Acoustic Alert** | **Acoustic Alert** |
| **Tracker (Remote)** | **Tracker: Lift** | **Lift** |
| **Tracker (Remote)** | **Tracker: Tamper Detected** | **Tamper Detected** |

## Telemetry (No Changes Proposed)
The following labels remain as they are in the current version:
*   `RTT (Tracker)`
*   `Tracker Accuracy`
*   `Viewer Accuracy`
*   `TRK` / `VIEW` labels in status rows.

## Forensic Unification
As of v8.8.35, the forensic model has been simplified. Legacy version tags have been removed from data models.

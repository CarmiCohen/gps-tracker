# Feature Elaborations

### Visualizing Forensic Snapshots (Issue #223 & #225)
This feature turns the "black box" data into something you can actually see. When the tracker detects a sudden "jump" or a GPS stall, it captures a snapshot of the satellite signal quality (SNR) and the device's physical vibration at that exact moment. By updating the Log Viewer, we allow you to click on an event and see these forensics, helping you understand if a location error was caused by high-interference environments or physical tampering.

### Ribbon Enhancements (Issue #224)
The "analytical ribbons" are the visual indicators at the top of the app. We've added "Tilt Stability" and "Baro Stability" to these ribbons. Think of this as a "heartbeat" for the device's orientation and altitude sensors. It allows you to see at a glance if the device is stable, tilting, or changing height (like in an elevator), which is critical for verifying the sensor fusion logic.

### Actionable Intelligence (Issue #221)
We've improved the "Location Pending" state on the map. Instead of just showing a generic loading indicator, the app now uses Bayesian logic to grow the uncertainty radius dynamically. This clearly communicates to the user that the system is losing confidence in the position, and it differentiates between a standard GPS stall and a potential security violation (like acoustic interference).

### Ghost Mode Polishing (Issue #222 & #227)
"Ghost Mode" handles the "hindsight" logic—where the system retroactively corrects a previous location after getting better data. To make this feel less jarring, we implemented Slate500 "Ghost Paths." Instead of markers suddenly "snapping" to a new spot, they fade in and interpolate smoothly, making the self-correction process look like an intentional high-tech feature rather than a software glitch.

### UX Clarity (Issue #226)
"Location Pending" states can be frustrating if you don't know why they are happening. We've updated the UI to show the specific cause of uncertainty. Instead of a vague "Searching..." message, the user might see "GPS Chip Stalled" or "Acoustic Interference Detected," turning a technical delay into actionable forensic intelligence.

---

# Issues #228 to #245: Simplified Explanations

### FIXED Issues
*   **Issue #228 & #229: Constant Synchronization**: We cleaned up the "Source of Truth" (SoT) documentation to make sure all parts of the app use the exact same rules for GPS stability and timing.
*   **Issue #230: Alert Text Fix**: Updated the "Chair Alert" subtitle to be more professional and consistent with the rest of the app's terminology.
*   **Issue #231: Visual Jump Alarm**: Fixed a bug where the "Visual Jump" alarm (triggered when the tracker appears to move impossible distances) wasn't actually firing when it should.
*   **Issue #232: UI Scaling Constant**: Added a missing technical value (`RIBBON_CURRENT_SCALE_MA`) to ensure the power/current indicators on the map are drawn at the correct size.

### OPEN Issues
*   **Issue #239: Hindsight Buffer Bug**: There is a small bug where old location data isn't being cleared properly after a correction, which can cause the phone to do unnecessary extra work.
*   **Issue #240: Missing Reason on Viewer**: The main tracker knows *why* a location is uncertain (e.g., GPS stall), but it's not sharing that specific reason with the "Viewer" app yet.
*   **Issue #241: Alarm Forensics Gap**: Currently, when an alarm goes off, it isn't saving the "Black Box" snapshots (like signal strength). This makes it harder to investigate the cause of the alarm later.
*   **Issue #242: Extra Math in Viewer**: The Viewer app is recalculating some sensor data that the Tracker has already finished, which is redundant and can be simplified.
*   **Issue #243: Sensitivity Gating**: Some small "visual jitters" are being ignored by the alarm system. We need to adjust the settings so these smaller events still trigger the appropriate warnings.
*   **Issue #244: Offline Data Loss**: When the tracker is offline and saving data to its internal memory, it "forgets" the specific reason for location uncertainty by the time it uploads to the server.
*   **Issue #245: Double Logging**: A bug is causing some sensor events to be recorded twice, which clutters the history logs with redundant information.

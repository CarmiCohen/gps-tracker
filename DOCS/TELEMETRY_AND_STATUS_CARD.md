# Telemetry & Status Card (v8.9.78)

The Status Card is the primary HUD for real-time tracking health. It utilizes the jitter-buffered 15s staleness threshold for all visual indicators.

## 1. Visual Freshness Indicators
- **Visual Heartbeat**: A circular progress indicator that shrinks as data ages (15s for Position / Sensor packets).
- **Ghost Mode UX**: HUD metrics, accuracy circles, and dashboard fields enter a dimmed "Ghost" state (Slate500) if telemetry is > 15s old.
- **Link Quality**: The "DAT" (Data) badge transitions to FAIL if the link is stale or the watchdog expires.

## 2. HUD Fields
- **GPS Age**: Real-time counter showing the age of the last coordinate fix.
- **Comm Index**: 10-bar signal indicator based on RTT and signal strength.
- **Dual-Metric Accuracy**: Side-by-side display of raw GPS accuracy and engine-filtered maxAccuracy.
- **Stationary Anchor Lock (Issue #018)**: Displays a **"LOCKED"** badge when the Behavioral Engine has clamped coordinates to a stationary anchor to prevent drift.

## 3. Forensic Transparency
- All telemetry fields support long-press for technical detail (if available).
- Signal and Accuracy values are color-coded based on behavioral sanity (e.g., Amber for high uncertainty).

# Analytical Ribbons & Forensic UI (v8.9.54)

The **Analytical Ribbons** provide high-density sparkline visualizations for forensic telemetry auditing, allowing monitoring of sensor trends over time. In v8.9.54, these ribbons are hardened with a 15s jitter-buffered staleness threshold.

## 1. The Ribbon Pipeline
Telemetries are sampled and down-sampled into six time-scales:
- **4M (4 Minutes)**: 1s resolution.
- **16M (16 Minutes)**: 4s resolution.
- **1H (1 Hour)**: 15s resolution.
- **4H (4 Hours)**: 60s resolution.
- **24H (24 Hours)**: 6m resolution.
- **7D (7 Days)**: 45m resolution.

## 2. Monitored Metrics
Each "Ribbon" visualizes a specific forensic metric:
- **SNR (Satellite Signal)**: Normalized to 45dB floor. Detects jamming or foliage occlusion.
- **Noise (Acoustic)**: Shows ambient noise floor vs. spikes.
- **Lux (Light)**: Logarithmic scale of ambient light exposure.
- **Vibe (Vibration)**: Normalized g-force magnitude.
- **Lift (Barometric)**: Vertical displacement trends.
- **CUR (Power Current)**: Real-time battery drain/charge in mA (Issue #337).
- **TLT (Tilt)**: Device orientation stability.
- **BAR (Baro Stability)**: Long-term pressure trends.
- **BAT (Battery Health)**: Highlights steep discharge events.

## 3. UI Implementation
- **Ghost Mode (Issue #338/428)**: Sparklines dim (Slate500) if the remote device is offline > 15s (`TELEMETRY_UI_STALE_THRESHOLD_MS`).
- **Forensic Parity (Issue #325)**: Every ribbon point carries both raw `accuracy` and authoritative `maxAccuracy` metadata to ensure the visual uncertainty matches the engine's state.
- **Forensic Tagging**: Every point includes battery current (`currentMa`), speed, bearing, and SIT status.
- **Interaction**: Tapping a ribbon expands it to a full-screen historical view with coordinate-aware scrubbing.

## 4. Forensic Continuity
Ribbons are reconstructed from the `HistoryEntity` database during session resumption.
- **Gap Handling**: Missing periods are visualized as "Gaps" in the ribbon to distinguish between "Stationary/Silent" and "Service Offline."
- **Log Spatial Anchor**: Historical ribbon points can be correlated with forensic logs using shared timestamps and Dual-Metric anchors (Issue #325).
- **Monotonic Integrity**: Ribbon sampling and gap detection utilize `TimeProvider.elapsedRealtime()` to prevent distortion from system clock resets.

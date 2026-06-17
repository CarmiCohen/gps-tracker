# Analytical Ribbons & Connection History (v8.8.35)

This document describes the visual telemetry and analytical "Ribbon" mechanism used to provide a high-fidelity history of the tracker's connectivity and signal quality.

## 1. The "Ribbon" Concept
The **Analytical Ribbons** (`AnalyticalRibbons`) are high-density timeline visualizations representing system health across multiple time scales.

### A. Density & Resolution
Each ribbon represents exactly **240 data points**.
- **4M (4 Minutes)**: 1 point = 1 second. High-resolution stability (`TICK_INTERVAL_MS`).
- **16M (16 Minutes)**: 1 point = 4 seconds.
- **1H (1 Hour)**: 1 point = 15 seconds.
- **4H (4 Hours)**: 1 point = 1 minute.
- **24H (24 Hours)**: 1 point = 6 minutes.
- **7D (7 Days)**: 1 point = 45 minutes (2700s). Corrected for exact midnight alignment (32 intervals/day).

### B. Power-Save Continuity (R880)
When the application is in the background and stationary, the system pulse throttles to **5 seconds** (`TICK_INTERVAL_SLOW_MS`) to conserve battery.
- **Analytical Backfill**: To prevent "stepped" updates or visual gaps in the 4M ribbon, the `HistoryManager` automatically performs a 1Hz backfill of the intermediate points using the latest telemetry data.
- **Benefit**: Users see a continuous, smooth 1Hz timeline in the UI, while the CPU benefits from a 5x reduction in background wakeups.

## 2. Rendering Mechanism (`ConnectionQualityRibbon`)
Ribbons are rendered using the **Jetpack Compose Canvas API**.
- **Worst-Case Aggregation**: Long-term ribbons (16M to 7D) capture momentary health drops (Max RTT, Min Signal, Min GPS-Index) rather than point-in-time samples.
- **Partitioning (R830)**: The 7D ribbon uses midnight-aligned segments and date labels (`dd/MM`).
- **Forensic SNR Ribbon**: A dedicated "SNR" sensor ribbon tracks Signal-to-Noise Ratio (`snrIdx`) over time to identify environmental GPS interference.

## 3. Data Collection & Persistence
- **Live Stream**: Points are emitted at 1Hz (either via live pulse or backfill) via `SharedFlow` for real-time UI.
- **Batch Processing**: History data is buffered and flushed to the database every **5,000ms** (`HISTORY_BATCH_WRITE_INTERVAL_MS`) or upon reaching **100 entries** (`HISTORY_BUFFER_MAX_SIZE`) to optimize I/O overhead.
- **Pruning (PERF_IO)**: Pruning is triggered globally by `MainRepository` after every 50 writes (`DB_PRUNE_THRESHOLD`) to minimize WAL pressure.
- **Gap Detection**: The system backfills "Gap" points (Black) if a significant temporal jump (> 10s `REAL_TIME_GAP_LIMIT_MS`) is detected.

## 4. UI Forensics

### A. Version Identification
The build version (v8.8.35) is displayed in the UI to allow screenshot evidence to be mapped to specific build baselines.

### B. Interactive GNSS Detail
Tapping the "Sats" or "GPS Index" card launches the **GNSS Detail Overlay**, providing a per-satellite breakdown of constellation type and SNR (dB-Hz).

## 5. Performance Optimization
To prevent root-level recomposition at 1Hz, the UI utilizes **isolated StateFlows** and **primitive state holders**:
- `history4MFlow`, `history16MFlow`, etc., are collected directly by the Canvas components.
- This ensures only the relevant ribbon redraws when new telemetry arrives.

## 6. Forensic Unification
Legacy version tags (`ver`, `vid`) have been removed from data models and database schemas in v8.8.35 to simplify the forensic model while maintaining high-fidelity analytical depth.

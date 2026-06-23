### 1. Log Viewer Interactivity
Yes, clicking on a log event opens a **LogDetailPane**. This pane displays "FORENSIC DETAIL," including SNR/Vibration snapshots, exact coordinates, and accuracy at the moment the log was generated. It also displays persistence metrics if the event was part of a repeated sequence.

### 2. Tilt & Baro Stability (Issue #224)
These appear as high-density sparklines in the Analytical Ribbons overlay:
- **TLT (Tilt)**: Indigo line (`#818CF8`) tracking angular changes.
- **BAR (Barometric)**: Teal line (`#2DD4BF`) tracking altitude stability.

### 3. Actionable Intelligence (Issue #221)
On the map, this looks like a **dynamic accuracy circle** that grows in size. As the location becomes "Pending," the circle scales outward based on a Bayesian growth rate, visually showing the increasing area of uncertainty over time.

### 4. Cause of Uncertainty (Issue #226)
The specific reason for uncertainty (e.g., "GPS STALL", "SIGNAL LOSS") is displayed as an **Amber text overlay** in the center of the map view. Additionally, a small **"P" badge** appears next to the device ID in the Status Bar to indicate the location is pending.

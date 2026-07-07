# HUD LED Specifications (v9.2.7)

This document serves as the definitive specification for HUD indicators, standardized under **Requirement R960**.

## 1. Upper Row (Global Status Bar)
This row provides a binary "at-a-glance" status of the local device health and immediate peer presence.

### Local Capability Strip (Hardware Group)
Standardized under **R960**, these three badges group the fundamental local hardware dependencies. They refer strictly to the **hardware currently being held** (the device in your hand).

| Name | Appearance (Active / Inactive) | Meaning | Condition |
| :--- | :--- | :--- | :--- |
| **INT** | Green (**BrandJd**) / Red (**Rose500**) | Internet | Active when the local device has internet access. |
| **SRV** | Green (**BrandJd**) / Red (**Rose500**) | Relay Server | Active when connected to the signaling server. |
| **GPS** | Green (**BrandJd**) / Red (**Rose500**) | **Local** GPS | Reflects the **local** device's GPS fix (Fresh if age < 10s). |

### Peer Presence Group
Reflects the immediate status of the monitoring link and the remote entity.

| Name | Appearance (Active / Inactive) | Meaning | Condition |
| :--- | :--- | :--- | :--- |
| **TRK / VWR** | Green (**BrandJd**) / Red (**Rose500**) | Peer Activity | **Viewer Mode (TRK):** Telemetry received in last 10s.<br>**Tracker Mode (VWR):** Viewer is actively polling. |
| **DAT** | Green (**BrandJd**) / Red (**Rose500**) | Data Integrity | (Viewer Mode Only) True if internet, relay, and peer are all active. |

### Safety & Integrity Group (Dynamic Center-Left)
These badges are injected dynamically immediately following the **Peer Presence** group. They use a compact `7.sp` font and `2.dp` padding to ensure fitment on all supported displays.

| Name | Appearance | Meaning | Condition |
| :--- | :--- | :--- | :--- |
| **ALM** | Pulsing Red (**Rose500**) | Alarm Latch | Visible if any unresolved alarms exist in the background. |
| **LOCKOUT** | Gray Box (**Slate500**) | UI Suppressed | A violation is active, but the user manually minimized the Red Overlay. |
| **SIREN LOCKOUT**| Pulsing Red Box | Audio Active | Red Overlay is minimized but the **Siren is still playing**. |
| **WATCHDOG** | OK (Green) / FAIL (Red) | Logic Pulse | Status of internal application monitoring services. |

### Movement & Speed (Tracker-Dependent Right Group)
Reflects the state of the entity being tracked (remote peer or self-focus).

| Name | Appearance | Condition |
| :--- | :--- | :--- |
| **State Label** | Green / Gray (**Slate500**) | Shows `MOVING` (pulsing) or `STATIONARY`. Turns **Gray** if Tracker GPS is stale. |
| **Speed Value** | Green / Gray (**Slate500**) | Real-time speed. Turns **Gray** and drops to `0.0` if Tracker GPS is stale. |

---

## 2. Viewer Row (Viewer Mode Only)
Describes the Viewer's local telemetry metrics. Base color: **Cyan (ViewerCyan)**.

*   **Label (VIEW ID):** First 6 characters of the Viewer ID.
*   **"P" Badge (Amber):** Visible if local location is "Pending" (GPS_GAP/JAMMER).
*   **Battery:** Cyan. Turns **Red** < 20%. **Amber Bolt** icon appears when charging.
*   **Temp (°):** Local battery temperature.
*   **Comm Bar:** 10-segment local connectivity index.
*   **Sats (X/Y):** Satellites Used / In View.
*   **Age:** Time since last local GPS fix. Turns **Gray** if > 10s.
*   **Accuracy:** Local precision. Turns **Gray** if GPS is stale.
*   **Distance:** Distance from Tracker to Viewer (Metric calculated locally).

---

## 3. Tracker Row
Describes the Tracker's telemetry metrics. Base color: **Green (BrandJd)**.

### Mode-Aware Binding Authority (R049)
*   In **Tracker Mode**, this row binds to the **local** hardware.
*   In **Viewer Mode**, it binds to the **remote** telemetry pulses.

### Metrics & Gates
*   **Waiting State:** Shows **">>> WAITING FOR TELEMETRY <<<"** in pulsing Gray if no data has arrived.
*   **Connectivity Gate:** If telemetry packets stop (>10s), Label, Battery, Temp, and Comm bars turn **Gray (Slate500)**.
*   **GPS Gate:** If the Tracker specifically loses its GPS fix (but link is still active), **only** Age and Accuracy turn **Gray**. Battery and Comm stay green.
*   **Distance:** Distance from Tracker to its designated "Home" geofence origin.

---

## Rationale for Redundancy (Issue #044)
The **GPS LED** (Row 1) and the **Age/Accuracy** text (Rows 2/3) are intentionally redundant to create a status hierarchy:
1.  **The Hardware Go/No-Go (Upper Row)**: Logical consistency. Tells the user immediately if the device in their hand is currently capable of accurate positioning/geofencing.
2.  **The Forensic Audit (Rows 2/3)**: Provides the specific metadata (staleness and precision) required to explain *why* the hardware status has changed.

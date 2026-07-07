# HUD LED Specifications (v9.2.6)

This document describes the names, appearance, meaning, and display conditions for all LEDs and status indicators on the HUD.

## 1. Upper Row (Global Status Bar)
Represents the local device health and the immediate presence of the peer.

| Name | Appearance (Active / Inactive) | Meaning | Condition |
| :--- | :--- | :--- | :--- |
| **INT** | Green (**BrandJd**) / Red (**Rose500**) | Internet Connectivity | Active when the local device has internet access. |
| **SRV** | Green (**BrandJd**) / Red (**Rose500**) | Relay Server | Active when connected to the signaling/relay server. |
| **TRK / VWR** | Green (**BrandJd**) / Red (**Rose500**) | Peer Activity | **Viewer Mode (TRK):** Telemetry received in last 10s.<br>**Tracker Mode (VWR):** Viewer is actively polling. |
| **DAT** | Green (**BrandJd**) / Red (**Rose500**) | Data Integrity | (Viewer Mode Only) True if internet, relay, and telemetry are all active. |
| **GPS** | Green (**BrandJd**) / Red (**Rose500**) | **Local** GPS | Reflects the **local** device's GPS fix health (Fresh if age < 10s). |
| **ALM** | Pulsing Red (**Rose500**) | Active Alarms | Visible only when there are unresolved alarms. |
| **LOCKOUT** | Gray (**Slate500**) / Pulsing Red | Suppression | Visible if an alarm is active but the red overlay is suppressed. |
| **WATCHDOG** | OK (Green) / FAIL (Red) | System Integrity | Status of internal application monitoring services. |

### Movement & Speed (Tracker-Dependent)
| Name | Appearance | Condition |
| :--- | :--- | :--- |
| **State Label** | Green / Gray (**Slate500**) | Shows `MOVING` (pulsing) or `STATIONARY`. Turns **Gray** if Tracker GPS is stale. |
| **Speed Value** | Green / Gray (**Slate500**) | Real-time speed. Turns **Gray** and drops to `0.0` if Tracker GPS is stale. |

---

## 2. Viewer Row (Viewer Mode Only)
Describes the Viewer's local telemetry. Base color: **Cyan (ViewerCyan)**.

*   **Label (VIEW ID):** First 6 characters of the Viewer ID.
*   **"P" Badge (Amber):** Visible if local location is "Pending" (GPS_GAP/JAMMER).
*   **Battery:** Cyan. Turns **Red** < 20%. **Amber Bolt** icon appears when charging.
*   **Temp (°):** Local battery temperature.
*   **Comm Bar:** 10-segment local connectivity index.
*   **Sats (X/Y):** Satellites Used / In View.
*   **Age:** Time since last local GPS fix. Turns **Gray** if > 10s.
*   **Accuracy:** Local precision. Turns **Gray** if GPS is stale.
*   **Distance:** Distance from Tracker to Viewer.

---

## 3. Tracker Row
Describes the Tracker's telemetry. Base color: **Green (BrandJd)**.

### Mode-Aware Binding Authority (R049)
In **Tracker Mode**, this row binds to the **local** hardware. In **Viewer Mode**, it binds to the **remote** telemetry.

### Viewer Mode (Remote Monitoring)
*   **Waiting State:** Shows **">>> WAITING FOR TELEMETRY <<<"** in pulsing Gray if no data is present.
*   **Connectivity Gate:** If telemetry stops (>10s), Label, Battery, Temp, and Comm bars turn **Gray (Slate500)**.
*   **GPS Gate:** If the Tracker loses GPS fix, Age and Accuracy turn **Gray**, regardless of link status.
*   **Distance:** Distance from Tracker to its designated "Home" point.

### Tracker Mode (Local Monitoring)
*   **Self-Focus:** Mirrors Viewer Row logic but represents the Tracker's own local hardware.
*   **"P" Badge / JAMMER:** Indicators are driven by the local GPS engine. False "JAMMER" labels are prevented by binding to `localLocation` instead of remote state.
*   **Differences:**
    *   Uses **Green (BrandJd)** base color.
    *   `DAT` badge is omitted from the upper row.
    *   Upper row `VWR` badge indicates if a remote Viewer is currently monitoring.

---

## Detailed Logic Breakdown by Mode

### 1. Upper Row (Viewer Mode)
* **LEDs:** `INT`, `SRV`, `TRK`, `DAT`, `GPS`, `ALM`, `WATCHDOG`.
* **Conditions:**
    * **Green:** Local internet OK, Server connected, Tracker active (<10s), Data healthy, Local GPS fix fresh (<10s).
    * **Red:** Any of the above failed/disconnected.
    * **Pulsing Red:** `ALM` appears if there are unresolved alarms.

### 2. Upper Row (Tracker Mode)
* **Differences:**
    * **Label Change:** `TRK` becomes `VWR` (indicates if a Viewer is actively watching/polling the Tracker).
    * **Missing LED:** `DAT` is hidden.
    * **GPS:** Refers to the Tracker’s own local GPS fix health.

### 3. Viewer Row (Always Viewer Mode)
* **Availability:** This row **only exists** in Viewer mode.
* **Appearance:** All healthy indicators are **Cyan** (`ViewerCyan`).

### 4. Tracker Row (in Viewer Mode)
* **Status:** Represents the **remote** device.
* **GPS Gate:** If the link is OK but Tracker GPS is lost, only `Age` and `Accuracy` turn gray; `Battery` and `Comm` stay green.

### 5. Tracker Row (in Tracker Mode)
* **Status:** Represents the **local** device.
* **Logic:** Prevents stale/remote "P" or "JAMMER" markers from appearing when local GPS hardware is healthy but no peer is connected.

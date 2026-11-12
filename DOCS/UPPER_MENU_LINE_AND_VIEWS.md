# Upper Menu Line & Quick Navigation Views Specification (vNov12.3)

This document specifies the upper menu line (top control bar) across the Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications. Since Nov12.3 every button does what it says (ISSUE-NOV10.8-O76, SOT 1.422).

## 1. Upper Menu Line Layout & Quick Navigation Icons
- **Setup (`Build` - Cyan)**: Opens the Setup & Info dialog (`showSetupDialog`), displaying device IDs, relay URL, version and status lines.
- **Settings (`Settings` - White)**: Sensor: opens the Settings screen (IDs, relay URL, Alerts, Siren, Phone and Clean setup). Monitor (`Settings / Re-pair`): asks to unpair, with a confirmation; it is the only control that unpairs.
- **Map (`Place` - Green, Monitor only)**: returns the map to automatic centering. The Sensor has no map (user decision 2026-11-12).
- **Info (`Info` - Blue)**: opens the Setup & Info dialog; the detailed Info returns with ISSUE-NOV10.8-O84.
- **Close (`Close` - Rose)**: sends the app to the background, as Back; services keep running and nothing is unpaired.
- **Events and Graphs**: hidden until ISSUE-NOV10.8-O84 revives them. The Tools overlay was removed in Nov12.3 (see `TOOLS_MENU_AND_DIAGNOSTICS.md`).

# Simplicity Audit Ideas (Sep.11.41)

## 🎯 UI & State Simplification
*   **Idea #6 (Consolidated Dashboard State)**: Further merge `TrackerDashboard` parameters into a single `DashboardViewState`.
*   **Idea #7 (HUD State Logic Extraction)**: Move the logic for mapping `ConnectivitySuite` flows to `HudConnectivityState` directly into a dedicated mapper class.
*   **Idea #8 (Uniform Overlay Logic)**: Standardize the overlay animation and backdrop logic across all overlays using a shared `FullScreenOverlay` wrapper.

## 🛠️ Architectural Simplification
*   **Idea #9 (VitalityPulseFlow Utility)**: Standardize the "vitality pulse" pattern implemented for #915 into a reusable `Flow` extension or custom wrapper. This would encapsulate the periodic heartbeat emission alongside value changes, reducing boilerplate in `SystemStatusProvider` and `HardwareProvider` for future reactive components.

# Simplicity Audit Ideas (Sep.11.10)

## 🎯 UI & State Simplification
*   **Idea #6 (Consolidated Dashboard State)**: Further merge `TrackerDashboard` parameters into a single `DashboardViewState`. Currently, ~50 parameters are passed, which can be bundled to reduce the parameter surface and simplify the `TrackerScreen` signature (similar to the Map State Partitioning success).
*   **Idea #7 (HUD State Logic Extraction)**: Move the logic for mapping `ConnectivitySuite` flows to `HudConnectivityState` directly into a dedicated mapper class or the `ConnectivitySuite` itself to keep `MainViewModel` lean.
*   **Idea #8 (Uniform Overlay Logic)**: Standardize the overlay animation and backdrop logic across `LogOverlay`, `SettingsOverlay`, and `RibbonsOverlay` using a shared `FullScreenOverlay` wrapper to reduce boilerplate in `TrackerScreen` and `ViewerScreen`.

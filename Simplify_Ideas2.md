# Simplicity Audit Ideas (Sep.11.52)

## 🎯 UI & State Simplification
*   **Idea #6 (Consolidated Dashboard State)**: Further merge `TrackerDashboard` parameters into a single `DashboardViewState`.
*   **Idea #7 (HUD State Logic Extraction)**: Move the logic for mapping `ConnectivitySuite` flows to `HudConnectivityState` directly into a dedicated mapper class.
*   **Idea #8 (Uniform Overlay Logic)**: Standardize the overlay animation and backdrop logic across all overlays using a shared `FullScreenOverlay` wrapper.

## 🛠️ Architectural Simplification
*   **Idea #9 (VitalityPulseFlow Utility)**: Standardize the "vitality pulse" pattern implemented for #915 into a reusable `Flow` extension or custom wrapper.
*   **Idea #10 (Dedicated Hardware Handlers)**: Further decouple high-frequency sensor processing (e.g. Accelerometer) from low-frequency UI-driven hardware interactions by establishing a handler pool in `HardwareProvider`, ensuring budget SOCs (A15) never experience cross-domain scheduling jitter.
*   **Idea #11 (Unified Telemetry Auditor)**: Create a dedicated component for tracking continuity metrics to avoid manual counter increments across different backfill methods (`backfillAnalyticalGaps` and `fillRealGap`), centralizing the "4M" ribbon accounting logic.
*   **Idea #12 (Thread Priority Policy Manager)**: Establish a centralized policy manager for thread priorities to avoid hardcoded constants in `HardwareProvider` and other services, allowing for hardware-specific priority profiles (e.g., A15 vs S24).

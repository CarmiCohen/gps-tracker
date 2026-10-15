# Resolution Archive - GPS Tracker

## 🏁 Oct15.20: HUD Animation Gating (ISSUE-OCT15.20-F1)
- **Problem**: During Mali GPU driver anomalies, active UI animations in the Diagnostic HUD can exacerbate thread contention and lead to ANRs.
- **Root Cause**: Animations like `movingAlpha` and `alertAlpha` continued to execute on the UI thread even when the GPU was stalled.
- **Resolution**: Implemented animation gating in `OverlayComponents.kt` and `SharedUiComponents.kt`. Updated `HeaderBar`, `DashboardHeader`, and `StatusBar` to consume `isMaliAnomaly` and freeze animations into static states (using `snap()` or constant alpha) when driver stress is detected.
- **SOT Impact**: Hardened UI stability for budget hardware during forensic anomalies.

## 🏁 Oct14.1: Vibration Floor Jitter Hardening (ISSUE-OCT15.11-F1)
- **Problem**: `Vibration Floor` field showing very high values on some A15 diagnostics.
- **Resolution**: Implemented boundary clamping `[0.02, 2.0]` G in native (JNI) and JVM layers.

## 🏁 Oct14.1: Proximity Debounce Hardening (ISSUE-OCT15.10-F2)
- **Problem**: Extreme negative values in `Prox Debounce` on A15.
- **Resolution**: Fixed assignment typo and implemented JNI result validation with safety clamping.

---
*(Earlier entries archived)*

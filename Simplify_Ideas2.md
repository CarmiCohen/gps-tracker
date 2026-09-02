# Simplification Ideas (Sep.02.41)

*   **Idea #1: Unified Sentinel Config**. Move all hardcoded thresholds from `EngineConstants.kt` into a single `SentinelConfig` object that can be updated dynamically, reducing parameter passing in `MainAlarmLogic` (R2.3).
*   **Idea #237: Permission Intent Factory**. Consolidate all system setting intents into a single `IntentFactory` within `core:engine` or a dedicated UI helper to eliminate redundant URI logic in `MainActivity` (R896).
*   **Idea #238: Sensitivity Logic Encapsulation**. Move the linear mapping logic from `SentinelValidator` into a dedicated `SensitivityMapper` utility to keep the validator focused strictly on Boolean gates (R2.3).

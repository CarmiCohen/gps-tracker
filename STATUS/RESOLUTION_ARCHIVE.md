# Resolution Archive

...

## 🏁 OCT15.5: Mapper Consolidation (SIMP-OCT15.4-S1)
- **Problem**: Duplicate mapping logic for `HistoryBuffer` (forensic telemetry normalization) was spread across `MainRepository` and `StateSubscriptionUseCase`, increasing maintenance risk and potential for normalization drift.
- **Root Cause**: Architectural debt following the introduction of primitive-backed history buffers.
- **Remediation**: Centralized all `HistoryBuffer` transformation logic into `TelemetryMapper.kt`. Both live telemetry streams and database-sourced data now pass through a unified mapping authority.
- **Impact**: Ensured consistent telemetry normalization across the entire application. Reduced code duplication and improved architectural simplicity.
- **Files**: `TelemetryMapper.kt`, `MainRepository.kt`, `StateSubscriptionUseCase.kt`.

## 🏁 OCT15.4: Ribbon Selector Consolidation (SIMP-OCT15.2-S1)
- **Problem**: Forensic connection ribbons performed telemetry normalization (scaling values to 0.0-1.0) on the UI thread during every recomposition, causing unnecessary computational overhead and potential frame drops during high-frequency updates.
- **Root Cause**: UI components were responsible for data transformation logic that belongs in the data pipeline.
- **Remediation**: Moved normalization logic into `MainRepository` and `StateSubscriptionUseCase`. Added pre-calculated `FloatArray`s to `HistoryBuffer` to store normalized values. Refactored `SharedUiComponents` to consume these arrays directly.
- **Impact**: Eliminated runtime scaling calculations on the main thread. Hardened the primitive pipeline by ensuring all UI-ready data is pre-calculated in background threads.
- **Files**: `Models.kt`, `MainRepository.kt`, `StateSubscriptionUseCase.kt`, `SharedUiComponents.kt`.

...

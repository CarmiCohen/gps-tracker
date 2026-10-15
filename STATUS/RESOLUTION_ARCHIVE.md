# Resolution Archive

...

## 🏁 OCT15.4: Ribbon Selector Consolidation (SIMP-OCT15.2-S1)
- **Problem**: Forensic connection ribbons performed telemetry normalization (scaling values to 0.0-1.0) on the UI thread during every recomposition, causing unnecessary computational overhead and potential frame drops during high-frequency updates.
- **Root Cause**: UI components were responsible for data transformation logic that belongs in the data pipeline.
- **Remediation**: Moved normalization logic into `MainRepository` and `StateSubscriptionUseCase`. Added pre-calculated `FloatArray`s to `HistoryBuffer` to store normalized values. Refactored `SharedUiComponents` to consume these arrays directly.
- **Impact**: Eliminated runtime scaling calculations on the main thread. Hardened the primitive pipeline by ensuring all UI-ready data is pre-calculated in background threads.
- **Files**: `Models.kt`, `MainRepository.kt`, `StateSubscriptionUseCase.kt`, `SharedUiComponents.kt`.

## 🏁 OCT15.3: Map Rendering Primitive Integration (SIMP-OCT15.1-S2)
- **Problem**: `MapTrailSegment` used `List<GeoPoint>`, causing thousands of `GeoPoint` objects to be created in the `MapStateMapper` during every map update, leading to GC pressure and potential UI stalls.
- **Root Cause**: Use of object-based coordinate lists in the intermediate UI mapping pipeline.
- **Remediation**: Transitioned `MapTrailSegment` to use primitive `DoubleArray`s for coordinates. Updated `MapStateMapper` to perform segmentation without `GeoPoint` allocation. Updated `MapOverlayManager` to convert to `GeoPoint` lists in background coroutines only when a segment's checksum changes.
- **Impact**: Eliminated `GeoPoint` churn in the mapping logic. Reduced main-thread overhead for trail updates.
- **Files**: `Models.kt`, `MapStateMapper.kt`, `MapOverlayManager.kt`.

## 🏁 OCT15.2: Primitive History Buffers (SIMP-OCT15.1-S1)
- **Problem**: Connection history ribbons (SNR, Noise, Vibe, etc.) emitted `List<ConnectionPoint>`, causing O(N) object allocations and heavy GC pressure during long sessions on budget hardware.
- **Root Cause**: Reliance on object-based telemetry containers for high-frequency time-series data.
- **Remediation**: Implemented `HistoryBuffer` using primitive arrays (`LongArray`, `DoubleArray`). Refactored repository, use-case, and UI layers to consume buffers by index.
- **Impact**: Reduced object overhead from O(N) to O(1) per flow emission. Eliminated telemetry-driven UI stalls on Samsung A15.
- **Files**: `Models.kt`, `MainRepository.kt`, `MainUiState.kt`, `MainViewModel.kt`, `StateSubscriptionUseCase.kt`, `SharedUiComponents.kt`.

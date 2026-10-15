# Resolution Archive

...

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

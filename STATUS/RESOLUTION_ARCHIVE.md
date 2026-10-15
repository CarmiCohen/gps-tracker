# Resolution Archive

...

## 🏁 OCT15.2: Primitive History Buffers (SIMP-OCT15.1-S1)
- **Problem**: Connection history ribbons (SNR, Noise, Vibe, etc.) emitted `List<ConnectionPoint>`, causing O(N) object allocations and heavy GC pressure during long sessions on budget hardware.
- **Root Cause**: Reliance on object-based telemetry containers for high-frequency time-series data.
- **Remediation**: Implemented `HistoryBuffer` using primitive arrays (`LongArray`, `DoubleArray`). Refactored repository, use-case, and UI layers to consume buffers by index.
- **Impact**: Reduced object overhead from O(N) to O(1) per flow emission. Eliminated telemetry-driven UI stalls on Samsung A15.
- **Files**: `Models.kt`, `MainRepository.kt`, `MainUiState.kt`, `MainViewModel.kt`, `StateSubscriptionUseCase.kt`, `SharedUiComponents.kt`.

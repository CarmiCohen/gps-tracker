# Issue #526: Main-Thread Purity (R526)

## Status: Resolved (July.20.07)
## Requirement: R526

### Description
The application's Main thread was being blocked by heavy initialization of Database and Hardware Managers during cold start, leading to ANRs and frame skipping on low-end hardware (e.g., Samsung A15).

### Resolution
- **Async Initialization**: Offloaded Room database initialization and hardware sensor registration to `Dispatchers.IO`.
- **Hilt Lazy Injection**: Implemented `Lazy<T>` injection for heavy managers to defer their creation until after the first frame is rendered.
- **Startup Benchmarking**: Reduced cold-start main-thread blocking time to <200ms.

### Verification
- [x] No ANRs reported on Samsung A15 during cold boot tests.
- [x] UI remains responsive (60 FPS) during the service bootstrap sequence.

# Simplification Ideas 2

This document tracks architectural simplification opportunities identified during hardening cycles.

## 🟢 Lifecycle & Hardware
*   **Idea #233: Centralized Context Shadowing**. To eliminate `getPackageName` log spam (Issue #894), wrap the application context in a delegate that caches the package name at the entry point of all system service requests, preventing redundant IPC calls. (Sep.01.25).
*   **Idea #232: Centralized Timing Constants**. Move the hardware unregistration settling duration (800ms) and WorkManager initialization flags to `SignalingConstants.kt` to avoid hardcoded values across multiple providers. (Sep.01.24).
*   **Idea #231: Unify Forensic Timing Hooks**. The forensic timing implemented in `ManagedUnregistrationHelper` could be abstracted into a higher-level `DiagnosticScope` to allow consistent performance profiling across all background services, not just hardware disposal. (Sep.01.23).
*   **Idea #230: Centralized Lifecycle Sequencer**. Move the strict "Unregister -> Settle -> Terminate" pattern from `HardwareProvider.stop()` into a reusable `ManagedLifecycleHelper`. (Sep.01.22).

...
*(Total Ideas: 233)*

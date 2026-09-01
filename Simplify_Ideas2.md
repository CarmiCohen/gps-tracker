# Simplification Ideas 2

This document tracks architectural simplification opportunities identified during hardening cycles.

## 🟢 Lifecycle & Hardware
*   **Idea #234: Unified Shadowed Context Injection**. Instead of manual wrapping in each provider, provide `ContextShadow` as a Hilt binding for `@ApplicationContext` to ensure all system service interactions across the app are automatically optimized. (Sep.01.26).
*   **Idea #233: Centralized Context Shadowing**. (Implemented Sep.01.26). Wrap the application context in a delegate that caches the package name at the entry point of all system service requests, preventing redundant IPC calls. (Sep.01.25).
*   **Idea #232: Centralized Timing Constants**. Move the hardware unregistration settling duration (800ms) and WorkManager initialization flags to `SignalingConstants.kt` to avoid hardcoded values across multiple providers. (Sep.01.24).
*   **Idea #231: Unify Forensic Timing Hooks**. The forensic timing implemented in `ManagedUnregistrationHelper` could be abstracted into a higher-level `DiagnosticScope` to allow consistent performance profiling across all background services, not just hardware disposal. (Sep.01.23).

...
*(Total Ideas: 234)*

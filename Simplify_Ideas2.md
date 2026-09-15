# Project Simplification Ideas

1. **Signaling State Reduction**: Simplify the sealed class hierarchy in `ConnectivityEvent` by merging redundant state transitions.
2. **Context Shadowing Automation**: Explore a compiler plugin or KSP processor to automatically apply `@ShadowContext` logic to classes injected with `ApplicationContext`.
3. **Redundant Logic Pruning**: Conduct a deep audit of `ConnectivitySuite` to remove legacy backfill triggers that are now handled by the optimized 60s sync loop.
4. **Unified Power Policy**: Consolidate Android 15 power-awareness logic (Doze deferral, backoff) into a reusable `A15PowerPolicy` component to reduce duplication across background service modules.

... (Items 5-18 remain in backlog) ...

*(Idea #21 resolved in Sep.14.47: Decoupled signaling forensics into dedicated logger)*
*(Idea #3 resolved in Sep.14.50: Pruned redundant legacy keepalive identity sync logic)*
*(Idea #1 resolved in Sep.14.52: Simplified reactive signaling hierarchy and pruned redundant pulse events)*
*(Idea #19 & #20 resolved in Sep.14.54: Lifecycle-integrated version and documentation sync)*
*(Idea #4 resolved in Sep.15.02: Unified power policy consolidation into A15PowerPolicy component)*

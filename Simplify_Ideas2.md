# Project Simplification Ideas

1. **Signaling State Reduction**: Simplify the sealed class hierarchy in `ConnectivityEvent` by merging redundant state transitions.
2. **Context Shadowing Automation**: Explore a compiler plugin or KSP processor to automatically apply `@ShadowContext` logic to classes injected with `ApplicationContext`.
3. **Redundant Logic Pruning**: Conduct a deep audit of `ConnectivitySuite` to remove legacy backfill triggers that are now handled by the optimized 60s sync loop.
19. **Lifecycle-integrated version sync**: Integrate the `syncDocsVersion` task into the standard `assemble` or `preBuild` lifecycle to ensure documentation is always updated during the build process without manual invocation.

... (Items 4-17 remain in backlog) ...

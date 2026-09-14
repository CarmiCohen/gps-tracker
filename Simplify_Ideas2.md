# Project Simplification Ideas

1. **Signaling State Reduction**: Simplify the sealed class hierarchy in `ConnectivityEvent` by merging redundant state transitions.
2. **Context Shadowing Automation**: Explore a compiler plugin or KSP processor to automatically apply `@ShadowContext` logic to classes injected with `ApplicationContext`.
3. **Redundant Logic Pruning**: Conduct a deep audit of `ConnectivitySuite` to remove legacy backfill triggers that are now handled by the optimized 60s sync loop.
19. **Lifecycle-integrated version sync**: Integrate the `syncDocsVersion` task into the standard `assemble` or `preBuild` lifecycle to ensure documentation is always updated during the build process without manual invocation.
20. **Automated Build Integrity Verification**: Link the `verifyVersionIntegrity` task to the standard `check` or `preBuild` lifecycle to ensure version safety is audited automatically on every build.
*(Idea #21 resolved in Sep.14.47: Decoupled signaling forensics into dedicated logger)*
*(Idea #3 resolved in Sep.14.50: Pruned redundant legacy keepalive identity sync logic)*

... (Items 4-17 remain in backlog) ...

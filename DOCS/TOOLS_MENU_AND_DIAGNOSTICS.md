# System Tools & Diagnostics Menu (removed in Nov12.3)

The Tools option and its `ToolsOverlay` (Nov4.32: 100 Hz burst, database pruning, test violation, watchdog reset) were removed from both apps in Nov12.3: every action was empty or only wrote a log line (ISSUE-NOV10.8-O76, user decision 2026-11-10; SOT 1.343 retired by 1.422). The last version with the overlay is tag Nov12.2 (`core-ui/src/main/kotlin/com/jd/core/ui/ToolsOverlay.kt`). Diagnostics views (event log, detailed Info, graphs) return with ISSUE-NOV10.8-O84.

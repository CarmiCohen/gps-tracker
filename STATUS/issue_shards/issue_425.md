# Issue #425: R865 Color Non-Compliance
**Status**: Resolved (v8.9.48)
**Requirement**: R799e

## Description
Discovered that some UI elements were still using Emerald500 instead of the authoritative "Unified Identity Green" (JD Vivid Green #78BE20).

## Resolution
- Replaced Emerald500 with BrandJd (#78BE20) in all status badges, map tools, and sparkline foundations.
- Synchronized color definitions in `Color.kt` and `colors.xml`.

# Manifest of Documentation Integrity (July.26.04)

This document serves as the authoritative map of the issue tracking system, documenting intentional gaps, naming conventions, and historical discontinuities.

## 1. Intentional Numerical Gaps
The following numerical ranges in `STATUS/backlog_shards/` represent intentional jumps in numbering, deprecated legacy sequences, or undocumented historical gaps:
*   **001 - 004**: Preliminary design phases (no shards).
*   **006 - 009**: Internal prototype iterations (no shards).
*   **019, 040, 045, 050**: Minor internal task gaps.
*   **056 - 057, 060**: Internal stabilization gaps.
*   **064 - 066, 069 - 071**: Historical cleanup gaps.
*   **073 - 076, 078 - 091**: Bulk architectural transition gaps.
*   **093 - 096, 103, 109 - 112, 116**: Pre-hardening verification gaps.
*   **125 - 143, 145**: Historical gap (Legacy transition).
*   **192**: Discontinuity in task sequencing (verified).
*   **200 - 270**: Bulk jump for major architectural shift (Note: Shards #221, #263 retained as legacy anchors).
*   **273, 285, 296**: Documentation alignment gaps.
*   **303 - 309, 311 - 315**: Bulk developmental gaps (Engine v1 cleanup).
*   **317 - 336**: Pre-Engine v2 transition range.
*   **338 - 399**: Bulk jump for Engine v2 development (Note: Shard #400 retained as legacy anchor).
*   **401 - 434, 439 - 510**: Bulk jump for Forensic Hardening cycle (Note: Shards #460, #502-508 retained as legacy anchors).
*   **461 - 501**: Missing range (Note: Shard #461 is a critical documentation anchor but the physical shard is currently absent).

## 2. Naming Conventions
*   **Standard**: `issue_XXX.md` where XXX is the zero-padded 3-digit ID.
*   **Sub-issues**: `issue_XXXb.md`, `issue_XXXc.md`, etc.
*   **Exception Shards**:
    *   `issue_526_power.md`: Legacy naming for Power Optimization (retained for forensic continuity).
    *   `issue_120b.md`: Hilt Hardening sub-issue.

## 3. Parity Audit
*   **Historical Shards**: Verified for all issues prior to July.24.06.
*   **Recent Discontinuities**: As of July.26.04, several issues cited in `SOT_MASTER_REQUIREMENTS.md` and `issues.md` lack corresponding physical shards in `STATUS/backlog_shards/`. These are tracked in Section 4.
*   **Requirement Mapping**: Requirements R872 and R405c are mapped to shards or historical anchors.

## 4. Unresolved Parity Anomalies (Missing Shards)
The following issues are authoritative (cited in SOT or Issues.md) but lack physical shard files:
*   **Core Engine/Forensic**: #595, #589, #588, #590, #591, #461 (Anchor).
*   **Service/Reactive Migration**: #545b, #545c, #585, #586, #587.
*   **Performance/Optimizations**: #547b, #547c, #548, #550, #560, #560b, #560c, #565, #570, #570b, #575.

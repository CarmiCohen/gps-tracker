# Manifest of Documentation Integrity (July.24.06)

This document serves as the authoritative map of the issue tracking system, documenting intentional gaps, naming conventions, and historical discontinuities.

## 1. Intentional Numerical Gaps
The following numerical ranges in `STATUS/backlog_shards/` represent intentional jumps in numbering, deprecated legacy sequences, or undocumented historical gaps:
*   **001 - 004**: Preliminary design phases (no shards).
*   **006 - 009**: Internal prototype iterations (no shards).
*   **019, 045, 050**: Minor internal task gaps.
*   **056 - 057, 060**: Internal stabilization gaps.
*   **064 - 066, 069 - 071**: Historical cleanup gaps.
*   **073 - 076, 078 - 091**: Bulk architectural transition gaps.
*   **093 - 096**: Pre-hardening verification gaps.
*   **125 - 143**: Historical gap (Legacy transition).
*   **200 - 270**: Bulk jump for major architectural shift (Note: Shards #221, #263 retained as legacy anchors).
*   **338 - 399**: Bulk jump for Engine v2 development (Note: Shard #400 retained as legacy anchor).
*   **439 - 510**: Bulk jump for Forensic Hardening cycle (Note: Shards #460, #461, #502-508 retained as legacy anchors).

## 2. Naming Conventions
*   **Standard**: `issue_XXX.md` where XXX is the zero-padded 3-digit ID.
*   **Sub-issues**: `issue_XXXb.md`, `issue_XXXc.md`, etc.
*   **Exception Shards**:
    *   `issue_526_power.md`: Legacy naming for Power Optimization (retained for forensic continuity).
    *   `issue_120b.md`: Hilt Hardening sub-issue.

## 3. Parity Audit
*   **Active Shards**: 1:1 parity verified for all issues cited in `SOT_MASTER_REQUIREMENTS.md` and `issues.md` as of July.24.06 (including new shards #543-#547).
*   **Archived Shards**: Historical resolutions verified in `STATUS/backlog_shards/archive/`.
*   **Requirement Mapping**: Requirements R872 and R405c are now explicitly mapped to shards or historical anchors (#872 created for stealth).

# Manifest of Documentation Integrity (July.24.06)

This document serves as the authoritative map of the issue tracking system, documenting intentional gaps, naming conventions, and historical discontinuities.

## 1. Intentional Numerical Gaps
The following numerical ranges in `STATUS/backlog_shards/` represent intentional jumps in numbering or deprecated legacy sequences:
*   **001 - 004**: Preliminary design phases (no shards).
*   **006 - 009**: Internal prototype iterations (no shards).
*   **125 - 143**: Historical gap (Legacy transition).
*   **200 - 270**: Intentional range jump for major architectural shift.
*   **338 - 399**: Intentional range jump for Engine v2 development.
*   **439 - 510**: Intentional range jump for Forensic Hardening cycle.

## 2. Naming Conventions
*   **Standard**: `issue_XXX.md` where XXX is the zero-padded 3-digit ID.
*   **Sub-issues**: `issue_XXXb.md`, `issue_XXXc.md`, etc.
*   **Exception Shards**:
    *   `issue_526_power.md`: Legacy naming for Power Optimization (retained for forensic continuity).
    *   `issue_120b.md`: Hilt Hardening sub-issue.

## 3. Parity Audit
*   **Active Shards**: 1:1 parity verified for all issues cited in `SOT_MASTER_REQUIREMENTS.md` and `issues.md` as of July.24.06.
*   **Archived Shards**: Historical resolutions verified in `STATUS/backlog_shards/archive/`.

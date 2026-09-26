# 🟢 Guidelines for Implementation

The following rules **MUST** be followed strictly for every task:

1.  **Display Issue**: Explicitly display the selected issue details before initiating any fix.
2.  **Root-Cause Remediation**:
    *   **Architecture First**: Resolve issues using root-cause-oriented solutions that align with the project's architecture and long-term maintainability.
    *   **No Workarounds**: Avoid temporary mitigations. Rigorously remove all "leftovers" from previous or current implementations.
    *   **Simplicity**: Prioritize code and architectural simplicity.
3.  **Comprehensive Resolution**: Ensure the implementation is fully resolved by addressing:
    *   Missing functionality and unhandled edge cases.
    *   Unfinished integration points.
    *   Inconsistencies with requirements.
    *   Unintended side effects and remaining risks.
4.  **Record Concerns**: Document newly identified risks, defects, or inconsistencies in `issues.md`.
5.  **Mark Resolved**: Update the relevant status tracking files and mark fixed issues as resolved.
6.  **Continuous Handover**: Update `Handover.md` immediately after any modification to a `.kt` file.
7.  **Transparency**: Provide a brief explanation for every action performed.
8.  **Large File Protection**:
    *   Use `replace_text` for targeted updates in large files (especially `.md` and `.xml`) to prevent data loss or truncation.
    *   Never use summaries or placeholders when using `write_file`.
9.  **Completion Adherence**: Execute the **Completion Sequence** in strict order after finishing an issue.
10. **Strict Termination**: Once the completion sequence is finished, do not implement anything else. **STOP ALL PROCESSING.**

---

## 🏁 Completion Sequence

Perform these steps in strict sequence once an issue is resolved:

1.  **Integrity Audit**: Verify no `*.md` or `*.xml` files were truncated. Ensure all changes are consistent with existing code and documentation.
2.  **State Tracking Update**: Synchronize `issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, and `STATUS/RESOLUTION_ARCHIVE.md`. Assign issue numbers to all new concerns.
3.  **Dashboard Synchronization**: Update the **Hardening Progress Dashboard** in `issues.md` to reflect the **Current Audit Baseline** (SOT Rules/IDs, Resolved, Open, Testing, Simplification Ideas, and QA tasks).
4.  **App Build & Versioning**: Rebuild the application and increment the `versionName` in `app/build.gradle`.
5.  **Git Release Block**: Generate a Git command block for staging, committing, tagging the version, and pushing to the remote.
6.  **Simplicity Audit**: Evaluate potential architectural simplifications. Document these in the "Strategic Simplification Ideas" section of `issues.md` with unique IDs and significance levels (High, Medium, Low).
7.  **Rule 8 Verification**: Confirm that Rule 8 (Large File Protection) was strictly followed for all modifications.
8.  **Final Handover**: Update `Handover.md` with a comprehensive forensic state snapshot for the next session.
9.  **Session Termination**: Stop the chat immediately. Do not attempt further fixes.
10. **Audit Recalculation**: Display final metrics in the format:  
    **Current Audit Baseline: [SOT: X (Rules: Y, IDs: Z), Resolved: A, Open: B, Testing: C (Sub-items: D), Ideas: E, QA: F]**
11. **Final Stop**: After the audit recalculation, **STOP ALL PROCESSING.**

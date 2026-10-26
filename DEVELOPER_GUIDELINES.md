# 🟢 Guidelines for Implementation

The following rules **MUST** be followed strictly for every task:

1.  **Display Issue**: Explicitly display the selected issue details before initiating the issue's fixing.
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
9.  **Archival Integrity**: Ensure that all archived rules, historical baselines, and sunset requirements are explicitly preserved in their respective archive files. Pruning active files MUST NOT result in the loss of historical forensic data.
10. **Completion Adherence**: Execute the **Completion Sequence** in strict order after finishing an issue.
11. **Strict Termination**: Once the completion sequence is finished, do not implement anything else. **STOP ALL PROCESSING.**
12. **Traceability Rule**: Remember to add the issue # to the git and to the relevant doc files.

---

## 🏁 Completion Sequence

Perform these steps in strict sequence once an issue is resolved:

1.  **Integrity Audit**: Verify no `*.md` or `*.xml` files were truncated. Ensure all changes are consistent with existing code and documentation.
2.  **State Tracking Update**: Synchronize `issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, and `STATUS/RESOLUTION_ARCHIVE.md`. Assign issue numbers to all new concerns.
3.  **Dashboard Synchronization**: Update the **Hardening Progress Dashboard** in `issues.md` with the latest **Audit Baseline** entry, prefixed with the current **Version Name** (e.g., `Sep.27.9`).
4.  **App Build & Versioning**: Rebuild the application and increment the `versionName` in `app/build.gradle`.
5.  **Simplicity Audit**: Evaluate potential architectural simplifications. Document these in the "Strategic Simplification Ideas" section of `issues.md` with unique IDs and significance levels (High, Medium, Low).
6.  **Forensic Integrity Check (Rule 8)**: Rigorously verify all modified `.md` and `.xml` files for correctness, completeness, and the absolute absence of placeholders or summaries. Confirm that Rule 8 (Large File Protection) was strictly followed for all files changed during the last fix, implementation, or remediation.
7.  **Baseline Synchronization (R-ID 744)**: Synchronize the `STATUS/BASELINE/` directory with the current state of critical forensic documents for the version being closed.
8.  **Final Handover**: Update `Handover.md` with a comprehensive forensic state snapshot for the next session.
9.  **Session Termination**: Stop the chat immediately. Do not attempt further fixes.
10. **Audit Recalculation**: Display final metrics in the format:  
    **[Version Name]: [SOT Count: X (Rules: Y), Open: H:xx, M:xx, L:xx, Ideas: H:xx, M:xx, L:xx, Testing: C (Sub-items: D), QA: F]**  
    *(Example current baseline line: `- **Sep.27.9: [SOT Count: 171 (Rules: 41), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:4, Testing: 3 (Sub-items: 15), QA: 284]**`)*
11. **Git Release Block**: Generate a Git command block for staging, committing, tagging the version, and pushing to the remote. Remember to add the issue # to the git and to the relevant doc files.
12. **Final Stop**: After the Git Release Block, **STOP ALL PROCESSING.**

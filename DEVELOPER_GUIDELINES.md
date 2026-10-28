# 🟢 Guidelines for Implementation

The following rules **MUST** be followed strictly for every task:

1.  **Display Issue & Resumption**: Explicitly display the selected issue details before initiating the issue's fixing. **If a `chat_session.md` exists, it MUST be read, its context integrated, and the "Resumption Sequence" executed.**
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
8.  **Large File Protection & Git Hygiene**:
    *   Use `replace_text` for targeted updates in large files to prevent truncation.
    *   Never use summaries or placeholders when using `write_file`.
    *   **Git Hygiene**: Ensure no temporary files (`*.tmp`, `chat_session.md`) are committed to the repository.
9.  **Archival Integrity**: Ensure that all archived rules, historical baselines, and sunset requirements are explicitly preserved in their respective archive files. Pruning active files MUST NOT result in the loss of historical forensic data.
10. **Completion Adherence**: Execute the **Completion Sequence** in strict order after finishing an issue.
11. **Interruption Protocol**: If the session must be terminated before an issue is resolved, execute the **Interruption Sequence** immediately.
12. **Strict Termination**: Once a termination sequence is finished, do not implement anything else. **STOP ALL PROCESSING.**
13. **Traceability Rule**: Remember to add the issue # to the git and to the relevant doc files.
14. **Stealth Authority**: For all siren and alarm triggers, role-based suppression (Tracker vs Viewer) MUST be enforced at the engine level to ensure absolute operational stealth (R872).

---

## 🏁 Completion Sequence

Perform these steps in strict sequence once an issue is resolved:

1.  **Integrity Audit**: Verify no `*.md` or `*.xml` files were truncated. Ensure all changes are consistent with existing code and documentation.
2.  **Forensic Audit**: Run the forensic audit suite (`ProductionReadinessAuditTest`) and verify zero Davey events > 500ms and zero StackLog leaks on target hardware (A15).
3.  **State Tracking Update**: Synchronize `issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, and `STATUS/RESOLUTION_ARCHIVE.md`. Ensure all resolved issues are assigned correct version IDs.
4.  **Dashboard Synchronization**: Update the **Hardening Progress Dashboard** in `issues.md` with the latest **Audit Baseline** entry, prefixed with the current **Version Name** (e.g., `Oct28.26`).
5.  **App Build & Versioning**: Rebuild the application (including `platinum` and `release` variants) and increment the `versionName` in `app/build.gradle`.
6.  **Simplicity Audit**: Evaluate potential architectural simplifications. Document these in the "Strategic Simplification Ideas" section of `issues.md`.
7.  **Forensic Integrity Check (Rule 8)**: Rigorously verify all modified `.md` and `.xml` files. **Rigorously verify that all version titles and headers (e.g., in Handover.md, issues.md, SOT_MASTER_REQUIREMENTS.md) match the version being closed.**
8.  **Audit Recalculation**: Display final metrics in the format:  
    **[Version Name]: [SOT Count: X (Rules: Y), Open: H:xx, M:xx, L:xx, Ideas: H:xx, M:xx, L:xx, Testing: C (Sub-items: D), QA: F]**  
    *(Example: `- **Oct28.26: [SOT Count: 869 (Rules: 369), Open: H:0, M:0, L:0, Ideas: H:1, M:1, L:101, Testing: PASSED (Sub-items: 41), QA: 869]**`)*
9.  **Final Handover Update**: Update the root `Handover.md` with a comprehensive forensic state snapshot. 
    *   **Git Release Block**: Generate a Git command block at the **very top**. The commit message **MUST BE EXHAUSTIVE** and summarize **ALL** issues resolved in this version (consult `RESOLUTION_ARCHIVE.md`). **Issue IDs from any `chat_session.md` MUST be included.**
    *   **Resumption Focus**: Update `## 🎯 Next Resumption Focus`.
10. **Baseline Synchronization (R-ID 744)**: Create the directory `STATUS/BASELINE/[Version Name]/` and copy all final forensic documents (`issues.md`, `Handover.md`, `SOT_MASTER_REQUIREMENTS.md`, `RESOLUTION_ARCHIVE.md`) into it.
11. **Resumption Verification**: If the session was resumed from a `chat_session.md`, verify that all "Planned Remediation" tasks identified in that export are fully addressed and documented.
12. **Cleanup**: Remove `chat_session.md` from the project root.
13. **Final Stop**: Display the final Git command block and Audit Recalculation line, then **STOP ALL PROCESSING.**

---

## ⚠️ Interruption Sequence

Perform these steps if the session is terminated before an issue is resolved:

1.  **Partial Handover**: Update `Handover.md` with findings, planned remediation steps, and blockers.
2.  **Session Export**: Export the current chat history to `chat_session.md` in the project root.
3.  **State Snapshot**: Ensure `issues.md` reflects any newly identified concerns as "Open Gaps".
4.  **Final Stop**: Terminate the session with the message: **"Exported per user request. All processing stopped."**

---

## 🧩 Resumption Sequence

Perform these steps when starting a session if `chat_session.md` exists:

1.  **Context Loading**: Read `chat_session.md` and `Handover.md`.
2.  **State Verification**: Verify if the `Planned Remediation` steps in the export were already partially implemented.
3.  **Issue Validation**: Ensure the `## 🎯 Current Resumption Focus` in `issues.md` aligns with the export.
4.  **Traceability Hook**: Reference the specific Issue ID from the export in the first action performed.

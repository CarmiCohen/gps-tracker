//# 🟢 Guidelines for Implementation

The following rules MUST be followed strictly for every task:

1. **Display Issue**: Display the selected issue here before starting the fix.
2. **Root-Cause Remediation**: Remediate the issues using only root-cause-oriented solutions, keep consistency with the project's architecture, design principles, and long-term maintainability objectives. Avoid temporary mitigations or workaround-based implementations. Rigorously remove leftovers, and leftovers of the leftovers, etc. Try to keep the app simple.
3. **Record Concerns**: Document any newly identified concerns in `issues.md`. Concerns include - risks, defects, inconsistencies.
4. **Mark Resolved**: Record all fixed issues in the relevant status tracking file and mark them as resolved.
5. **Continuous Handover**: Update `Handover.md` after each modification to any `.kt` file.
6. **Transparency**: Briefly explain each action before executing it.
7. **Large File Protection**: Prioritize using `replace_text` for targeted updates in large files (especially `.md` and `.xml`) to prevent accidental data loss or truncation. Avoid using summaries or placeholders when using `write_file`.

## 🏁 Completion Sequence
Perform the following steps in order once an issue is resolved:

1. **Integrity Audit**: Verify that no `*.md` or `*.xml` files were accidentally truncated and ensure the change is consistent with existing code and documentation.
2. **State Tracking Update**: Update `issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, and `STATUS/RESOLUTION_ARCHIVE.md`. Record all fixed issues and verify that new concerns have issue numbers assigned.
3. **Dashboard Extension**: Extend and synchronize the **Hardening Progress Dashboard** in `issues.md` to match the **Current Audit Baseline** (SOT Rules, SOT IDs, Resolved, Open, Testing Chapters, Testing Sub-items, Simplification Ideas, and QA Validation tasks).
4. **App Build & Versioning**: Rebuild the app and update the `versionName` in `app/build.gradle`.
5. **Git Release Block**: Prepare a Git command block to stage, commit, tag the version, and push to the remote repository.
6. **Simplicity Audit**: Evaluate if the code or app architecture can be further simplified. Save these ideas in `Simplify_Ideas2.md`.
7. **Final Handover**: Update `Handover.md` with a comprehensive forensic state snapshot to prepare for a fresh chat session.
8. **Session Termination**: Stop the chat immediately. Do not attempt to fix any other issues or continue work.
9. **Audit Recalculation**: Recalculate all metrics and display them in the format: **Current Audit Baseline: [SOT: X (Rules: Y, IDs: Z), Resolved: A, Open: B, Testing: C (Sub-items: D), Ideas: E, QA: F]**.

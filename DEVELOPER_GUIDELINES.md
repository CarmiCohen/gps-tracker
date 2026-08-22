//# 🟢 Guidelines for Implementation

The following rules MUST be followed strictly for every task:

1. **Display Issue**: Display the selected issue here before starting the fix.
2. **Root-Cause Remediation**: Remediate the issues using only root-cause-oriented solutions, keep consistency with the project's architecture, design principles, and long-term maintainability objectives. Avoid temporary mitigations or workaround-based implementations. Rigorously remove leftovers, and leftovers of the leftovers, etc. Try to keep the app simple.
3. **Record Concerns**: Document any newly identified concerns in `issues.md`. Concerns include - risks, defects, inconsistencies.
4. **Mark Resolved**: Record all fixed issues in the relevant status tracking file and mark them as resolved.
5. **Continuous Handover**: Update `Handover.md` after each modification to any `.kt` file.
6. **Transparency**: Briefly explain each action before executing it.

## 🏁 Completion Sequence
Perform the following steps in order once an issue is resolved:

1. **Integrity Audit**: Verify that no `*.md` or `*.xml` files were accidentally truncated and ensure the change is consistent with existing code and documentation.
2. **State Tracking Update**: Update `issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, and `STATUS/RESOLUTION_ARCHIVE.md`. Record all fixed issues and verify that new concerns have issue numbers assigned.
3. **App Build & Versioning**: Rebuild the app and update the `versionName` in `app/build.gradle`.
4. **Git Release Block**: Prepare a Git command block to stage, commit, tag the version, and push to the remote repository.
5. **Simplicity Audit**: Evaluate if the code or app architecture can be further simplified. Save these ideas in `Simplify_Ideas2.md`.
6. **Final Handover**: Update `Handover.md` with a comprehensive forensic state snapshot to prepare for a fresh chat session.
7. **Session Termination**: Stop the chat immediately. Do not attempt to fix any other issues or continue work.
8. **Audit Recalculation**: Recalculate number of items for SOT, Resolved issues, open issues, Testing chapters, sub-items, simplification ideas, QA validation status, and display it in a format of: Current Audit Baseline.

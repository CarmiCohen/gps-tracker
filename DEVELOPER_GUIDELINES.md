# 🟢 Guidelines for Implementation

The following rules MUST be followed strictly for every task:

1. **Display Issue**: Display the selected issue here before starting the fix.
2. **Root-Cause Remediation**: Remediate the issues using only root-cause-oriented solutions, keep consistency with the project's architecture, design principles, and long-term maintainability objectives. Avoid temporary mitigations or workaround-based implementations. Rigorously remove leftovers, and leftovers of the leftovers, etc. Try to keep the app simple.
3. **Record Concerns**: Document any newly identified concerns in `issues.md`. Concerns include - risks, defects, inconsistencies.
4. **Mark Resolved**: Record all fixed issues in the relevant status tracking file and mark them as resolved.
5. **Continuous Handover**: Update `Handover.md` after each modification to any `.kt` file.
6. **Transparency**: Briefly explain each action before executing it.

## 🏁 Completion Sequence
Perform the full sequence once the specific issue is resolved:

- **a.** Rebuild the app.
- **b.** Verify that ALL fixed issues are updated in `issues.md` or another status tracking md file.
- **c.** Check that no `*.md` or `*.xml` file was accidentally truncated.
- **d.** Verify that there is no inconsistency with this change of the app and other code portions or documentation.
- **e.** Verify that new requirements are added to `STATUS/SOT_MASTER_REQUIREMENTS.md`.
- **f.** Prepare a block of Git commands to stage the changes and to commit them as a new release with a tag the version and to push everything to the remote repository. The subversion number will be incremented automatically.
- **g.** Please verify that `Handover.md` is updated and ready for new fresh chat.
- **h.** Documentation integrity: Is there anything else we should do in order to fix the Status tracking md files and keep the integrity?
- **i.** Please always set issue number to all "Newly Identified Risks & Concerns" in `issues.md`, and "Next Objective" in `Handover.md`.
- **j.** After completing the fix for the issue, stop the chat and do not attempt to fix any other issues.
- **k.** Recheck `issues.md`, `sot_master_requirements.md`, `resolution_archive.md`, and `handover.md`.
- **l.** Do not fix more than one issue, do not continue to the next issue: after completing the issue - stop this chat.

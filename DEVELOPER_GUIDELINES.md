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
4.  **Record Concerns**: Document newly identified risks, defects, or inconsistencies as one Open Gaps line in `issues.md` plus a section in `DOCS/issue_details.md`.
5.  **Mark Resolved**: Remove a resolved item from `issues.md` and its section from `DOCS/issue_details.md`; its record is the version's commit message (D1).
6.  **Continuous Progress Log**: `chat_session.md` is the milestone log (D6); changed files are logged by the hook.
7.  **Transparency**: Provide a brief explanation for every action performed.
8.  **Large File Protection & Git Hygiene**:
    *   Use targeted replacements in large files to prevent truncation.
    *   Never use summaries or placeholders when using `write_file`.
    *   **Git Hygiene**: Ensure no temporary files (`*.tmp`, `chat_session.md`) are committed to the repository.
9.  **Archival Integrity**: Frozen files are never edited or pruned: `STATUS/RESOLUTION_ARCHIVE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/BASELINE/INDEX.md` and the `STATUS/BASELINE/<version>/` folders (all frozen at Nov23.4, user decision 2026-11-23). The baseline of a version is its git tag (`git show <tag>:<path>`). Text pruned from a working file stays readable in the previous version's tag.
10. **Completion Adherence**: Execute the **Completion Sequence** in strict order after finishing a session's work.
11. **Interruption Protocol**: If the session must be terminated before the work is resolved, execute the **Interruption Sequence** immediately.
12. **Strict Termination**: Once a termination sequence is finished, do not implement anything else. **STOP ALL PROCESSING.**
13. **Traceability Rule**: Name the issue IDs in the commit message, in code comments of the change, and in `issues.md` while open. Code comments say what and why; history (dates, measurements, decisions) goes in the commit message (O171).
14. **Stealth Authority**: For all siren and alarm triggers, role-based suppression (Tracker vs Viewer) MUST be enforced at the engine level to ensure absolute operational stealth (R872).
15. **Testing**: Every feature, hardening update, or bug fix **MUST** come with automated unit or instrumented tests, named in the commit message. A chapter in `TESTS/TEST_PROCEDURE.md` is written **only** for a check that needs a person, a device or two phones and is still pending (user decision 2026-11-23); a changed result of an existing chapter updates only its `**Status:**` line.
16. **Build Warning Auditing**: Gradle builds run with `--warning-mode all` (through `TESTS/run_tests.ps1`); report any new deprecation or configuration warning from project code or scripts.
17. **No Test Removal Without Confirmation**: Do not remove any test, whether a chapter or sub-item of `TESTS/TEST_PROCEDURE.md` or an automated unit or instrumented test, without the user's explicit confirmation (user decision 2026-11-17).

---

## 📝 Documentation Rules (lean workflow, user decision 2026-11-23, Nov23.6-docs)

Every document line is paid for by each later session that reads it. Write each fact once, in the cheapest place, and read only what is needed. `TrackingDocumentIntegrityTest` enforces the limits marked *(tested)*.

*   **D1. The commit message is the record**: what changed, why, the main files, the tests and their results, at most 15 lines: title with version and issue IDs, one to three lines per issue, one line of tests and device results, the version. Git keeps it with the diff (`git log`, `git show <tag>`). No archive entry, SOT rule, dashboard row, audit line or index row is written any more; those files are frozen at Nov23.4 (Rule 9). *(tested: no archive version after Nov23.4)*
*   **D2. `DOCS/issues.md` is a short index of open work** (Nov27.5-docs, O171): the Resumption State (at most 8 bullets), the Status of the last version (at most 3 bullets), the Next Resumption Focus (at most 5 short lines), and one line per Work Queue item, Open Gap and Strategic Simplification Idea. The full text of each item is a section of `DOCS/issue_details.md` headed by its ID; device procedures are in `DOCS/operational_notes.md`. A resolved item is removed from both files. No history paragraphs, no dashboard. *(tested: the sections, no dashboard or Resolved Gaps, at most 12,000 characters; the two other files exist)*
*   **D3. No separate Handover**: `Handover.md` was merged into the index's Resumption State and Status in Nov27.5-docs (O171); no copy of the commit message there. *(tested)*
*   **D4. Git tags are the baselines**: `git tag` lists the versions; Claude tags each version at Completion step 5.
*   **D5. Docs-only versions** (no change under `app-*/src/main`, `core-*/src/main`, or build scripts) run only `:core-shared:test` plus the tests of any changed test module; no builds, no device audit. Small housekeeping (instruction files, queue edits) is committed with the next real version.
*   **D6. `chat_session.md` is a milestone log**, kept only in sessions that change code or device state (O171): decisions, test results, next steps, and any temporary device state that must be restored. Append short lines; do not rewrite it. Changed files are logged by the `PostToolUse` hook in `.claude/settings.json`.
*   **D7. Targeted reads**: find sections with search and read only the needed line ranges; do not re-read a file already known in the session; never read the frozen files whole. **Tests while working**: only the affected test classes (`--tests`), with quiet output (`-q`); the full suite once, at Completion step 1. **Device checks**: prefer `TESTS/device_check.ps1`, `REV_*` logcat lines or a text-only `uiautomator dump` over screenshots. `TESTS/TEST_PROCEDURE.md`: a new chapter goes right before the `---` line above `**Active Testing Chapters: X of Y**`, then that line is updated.


---

## 🏁 Completion Sequence

One Completion Sequence per session; it may close several small items together (user decision 2026-11-23).

1.  **Verification** (docs-only versions: see D5): `TESTS/run_tests.ps1 test assembleDebug assembleRelease` (one summary line; full log in `build/run_tests.log`); report any new warning. **Device audit only when the change touches app start-up, services or the first screen** (user decision 2026-11-23): on the A15 `ProductionReadinessAuditTest` by `am instrument`, then `TESTS/install_release.ps1` and `TESTS/device_audit.cmd` after a cold start (zero Davey events over 500 ms, zero StackLog leaks), then restore the debug builds with `gradlew :app-sensor:installDebug :app-monitor:installDebug` (only the apps installed there). Otherwise say it was not needed.
2.  **Version**: increment `appVersionName` and `appVersionCode` in `gradle.properties`, the single version source.
3.  **Working files**: update `issues.md` (remove resolved items, add new gaps and at most one simplification idea, update the Next Resumption Focus), `DOCS/issue_details.md` to match, a `TEST_PROCEDURE.md` chapter only under Rule 15, and the index's Resumption State and Status (D2).
4.  **Integrity check**: run `:core-shared:test`; check with `git --no-pager diff --numstat` that no `*.md` or `*.xml` file lost lines unexpectedly.
5.  **Commit, tag, push**: delete `chat_session.md` (after checking its planned tasks are done), write the commit message (D1) to a temp file, `git add -A`, `git commit -F <file>`, verify it, `git tag <Version>`, `git push origin main --tags`, without asking the user. If the push fails, report it; never force-push.
6.  **Final stop**: tell the user the issue IDs, the result, and the commit hash. **STOP ALL PROCESSING.**

---

## ⚠️ Interruption Sequence

1.  **Session log**: add the findings, the planned remediation steps, and the blockers to `chat_session.md` (D6).
2.  **Partial Handover**: add one line to the Resumption State of `DOCS/issues.md` pointing to `chat_session.md`.
3.  **State Snapshot**: ensure `issues.md` lists any newly identified concerns as Open Gaps.
4.  **Final Stop**: Terminate the session with the message: **"Exported per user request. All processing stopped."**

---

## 💡 Resumption Sequence

1.  **Context Loading**: read `DOCS/issues.md` (the index) and, if it exists, `chat_session.md`; then only the chosen item's section of `DOCS/issue_details.md` (D7).
2.  **State Verification**: verify whether the planned steps in `chat_session.md` were already partially implemented.
3.  **Traceability Hook**: reference the issue ID in the first action performed.

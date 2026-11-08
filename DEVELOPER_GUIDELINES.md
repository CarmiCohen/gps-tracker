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
5.  **Mark Resolved**: Update the relevant status tracking files and mark fixed issues as resolved, following the Documentation Rules below.
6.  **Continuous Progress Log**: After any modification to a `.kt` file, add one line to the `chat_session.md` log (D6). `Handover.md` is written once, at Completion step 7, or at the Interruption Sequence (changed in Nov8.100-Documentation; until then `Handover.md` was rewritten after every `.kt` change).
7.  **Transparency**: Provide a brief explanation for every action performed.
8.  **Large File Protection & Git Hygiene**:
    *   Use `replace_text` for targeted updates in large files to prevent truncation.
    *   Never use summaries or placeholders when using `write_file`.
    *   **Git Hygiene**: Ensure no temporary files (`*.tmp`, `chat_session.md`) are committed to the repository.
9.  **Archival Integrity**: Ensure that all archived rules, historical baselines, and sunset requirements are explicitly preserved in their respective archive files. Pruning active files MUST NOT result in the loss of historical forensic data. The baseline of a version is its git tag, listed in `STATUS/BASELINE/INDEX.md`; the old copies in the `STATUS/BASELINE/<version>/` folders are frozen and are never edited or deleted (ISSUE-NOV8.5-O47). Text pruned from a working file under D2 stays readable in the previous version's tag.
10. **Completion Adherence**: Execute the **Completion Sequence** in strict order after finishing an issue.
11. **Interruption Protocol**: If the session must be terminated before an issue is resolved, execute the **Interruption Sequence** immediately.
12. **Strict Termination**: Once a termination sequence is finished, do not implement anything else. **STOP ALL PROCESSING.**
13. **Traceability Rule**: Remember to add the issue # to the git and to the relevant doc files.
14. **Stealth Authority**: For all siren and alarm triggers, role-based suppression (Tracker vs Viewer) MUST be enforced at the engine level to ensure absolute operational stealth (R872).
15. **Testing & Test Procedure Synchronization**: Every implemented feature, hardening update, or bug fix **MUST** be accompanied by automated unit or instrumented tests and explicitly documented as a new test chapter in `TESTS/TEST_PROCEDURE.md` (incrementing total testing chapters accordingly).
16. **Build Warning Auditing**: During build verification phases, run Gradle builds with `--warning-mode all` (e.g., `./gradlew assembleDebug --warning-mode all`) to inspect deprecation warnings and ensure no new deprecations or configuration warnings are introduced by project code or scripts.

---

## 📝 Documentation Rules (user decision 2026-11-08, Nov8.100-Documentation)

Documents are written for the next session, which pays to read them. Write each fact once, keep working files short, and read only what is needed. `TrackingDocumentIntegrityTest` and `BaselineIndexTest` enforce the limits marked *(tested)*.

*   **D1. One home per fact**: the full description of a resolved issue (root cause, fix, files, tests, results) is written only in `STATUS/RESOLUTION_ARCHIVE.md`. Everywhere else it is one line with the issue ID and a pointer: the SOT rule states the requirement, the SOT Core Hardening Baseline and verification chapter are one line each, `TEST_PROCEDURE.md` describes what the test checks, and `Handover.md` and the commit message summarize. *(tested: every archived version appears in the SOT baseline and in a dashboard row)*
*   **D2. `DOCS/issues.md` is a working file**: it holds only the Next Resumption Focus, the open Work Queue rows, the Open Gaps, the Operational Notes, the Strategic Simplification Ideas, and the newest dashboard rows. A resolved item is removed (its text stays in the previous tag and its record in the archive); no Resolved Gaps sections. When the dashboard has more than 6 rows, the oldest moves unchanged to the Dashboard History at the end of the archive. Open and Ideas are counted from the lists in the file. *(tested: no Resolved Gaps, at most 6 dashboard rows, the dashboard last, at most 40,000 characters)*
*   **D3. Short Handover and commit message**: `Handover.md` follows the template below and stays within 70 lines *(tested)*. The commit message has at most 12 lines: title with version and issue IDs, one line per issue, tests, version. The final chat message does not repeat the Git block or the archive text; it names the issue IDs, the result, and points to `Handover.md`.
*   **D4. Git tags are the baselines**: no document copies; one `pending` row in `STATUS/BASELINE/INDEX.md` per version (Nov8.14). *(tested by `BaselineIndexTest`)*
*   **D5. Short Completion Sequence**: the 10 steps below. **Docs-only versions** (no change under `app-*/src/main`, `core-*/src/main`, or build scripts) run only `:core-shared:test` plus the tests of any changed test module at step 1; no builds, no device audit. **Small housekeeping** (instruction files, index rows, queue edits) does not get its own version: it is committed with the next real version (user decision 2026-11-08).
*   **D6. `chat_session.md` is a milestone log**: decisions, test results, next steps, and any temporary device state that must be restored. Append short lines; do not rewrite it. Changed files are logged automatically by the `PostToolUse` hook in `.claude/settings.json` (`.claude/hooks/log-change.ps1`, one `edited <path>` line per write while the log exists), so they are not written by hand; this also satisfies Rule 6.
*   **D7. Targeted reads**: find sections with search, read only the needed line ranges, use `git --no-pager` and file hashes or line counts for checks, and do not re-read a file whose content is already known in the session. **Tests while working**: run only the tests of the changed module (for example `:app-monitor:testDebugUnitTest --tests "*Geofence*"`); the full suite and the builds run once, at Completion step 1. **Device checks**: prefer `TESTS/device_check.ps1` (debug-build adb commands answering in one `REV_DEBUG_CMD` line), other `REV_*` logcat lines or a text-only screen dump (`adb shell uiautomator dump /sdcard/ui.xml` then search it for the expected text) over screenshots. The growing files are edited at fixed anchors, never read whole to find the place:
    *   `STATUS/RESOLUTION_ARCHIVE.md`: new version section right after the italic line under `# Resolution Archive`; oldest dashboard row moved to the top of `## 📊 Dashboard History`.
    *   `TESTS/TEST_PROCEDURE.md`: new chapter right before the `---` line above `**Active Testing Chapters: X of Y**`, then update that line; title line 1.
    *   `STATUS/SOT_MASTER_REQUIREMENTS.md`: title line 1; rule count in `## 🏗️ Architectural Master Rules (N Rules)`; new rule after the last `*   **1.` rule line (search `1.` + highest number); baseline line after `## 🛡️ Core Hardening Baseline`; chapter line after `## ✅ Verification Chapters`.
    *   `STATUS/BASELINE/INDEX.md`: new row right after `| :--- | :--- | :--- |`.
    *   `TESTS/CHAPTER_TRIAGE.md`: title line 1, the `**Summary:**` line, and the new row after the last chapter row (search the previous chapter number).

### Handover template (D3)
```
<Git block: git add . / git commit -m "<at most 12 lines>" / git tag <Version> / git push origin main --tags, one command per line, no &&>

# Project Handover - Version <Version> (<Title>)
## ▶️ Resumption State (Read First)      <- at most 8 bullets: git state, pending index row, user decisions, phones, test tips or a pointer to them
## 🟢 Status: PASSED (<Version>)          <- at most 5 bullets: issue IDs, result, what was not run and why; details: RESOLUTION_ARCHIVE.md
**Audit line**: <Version>: [SOT Count: ..., QA: ...]
## 🎯 Next Resumption Focus               <- at most 5 items
```

---

## 🏁 Completion Sequence

Perform these steps in strict sequence once an issue is resolved:

1.  **Verification** (docs-only versions: see D5): run the unit tests of all subprojects, and `assembleDebug` and `assembleRelease` with `--warning-mode all` (Rule 16), with `TESTS/run_tests.ps1 test assembleDebug assembleRelease` (one summary line; full log in `build/run_tests.log`); report any new warning. When app code changed, also run the forensic audit on the A15: `ProductionReadinessAuditTest` and `TESTS/device_audit.cmd` for both apps (zero Davey events over 500 ms, zero StackLog leaks); otherwise state that and why it was not run.
2.  **Version**: increment `appVersionName` and `appVersionCode` in `gradle.properties`, the single version source (SIMP-NOV7.0-S1).
3.  **Archive (D1)**: write the full description of each resolved issue, with its issue ID, as a new top section of `STATUS/RESOLUTION_ARCHIVE.md`.
4.  **Pointers (D1, D2)**: update `issues.md` (remove resolved rows and gaps, add new open gaps and simplification ideas from a short simplicity audit, update the Next Resumption Focus), the SOT (rule, one-line baseline entry, one-line verification chapter, header version and rule count), `TEST_PROCEDURE.md` and `CHAPTER_TRIAGE.md`, and the dashboard (new row on top; at most 6 rows).
5.  **Audit line**: compute it with `TESTS/audit_line.ps1 -Version <name>` and write it in the dashboard row in this format:
    **[Version Name]: [SOT Count: X (Rules: Y), Open: H:xx, M:xx, L:xx, Ideas: H:xx, M:xx, L:xx, Testing: C (Sub-items: D), QA: F]**
6.  **Baseline index (D4, R-ID 744)**: add `| [Version Name] | pending | |` at the top of the table in `STATUS/BASELINE/INDEX.md`.
7.  **Handover (D3)**: write `Handover.md` from the template, with the Git block at the very top (plain code block, each command on its own line, `git tag [Version Name]` and `git push origin main --tags` included, issue IDs from `chat_session.md` in the commit message).
8.  **Integrity check**: run `:core-shared:test` (`TrackingDocumentIntegrityTest`, `BaselineIndexTest`, `VersionSourceTest` must pass); check with `git --no-pager diff --numstat` that no `*.md` or `*.xml` file lost lines unexpectedly; verify that all version titles and headers match the version being closed.
9.  **Resumption verification and cleanup**: if the session resumed from `chat_session.md`, verify that its planned tasks are addressed and documented; then delete `chat_session.md`.
10. **Final stop**: tell the user the issue IDs, the result, the audit line, and that the Git block is at the top of `Handover.md` (D3). **STOP ALL PROCESSING.**

---

## ⚠️ Interruption Sequence

Perform these steps if the session is terminated before an issue is resolved:

1.  **Session log**: add the findings, the planned remediation steps, and the blockers to `chat_session.md` (D6).
2.  **Partial Handover**: update only the Resumption State of `Handover.md` with one line pointing to `chat_session.md`.
3.  **State Snapshot**: ensure `issues.md` lists any newly identified concerns as Open Gaps.
4.  **Final Stop**: Terminate the session with the message: **"Exported per user request. All processing stopped."**

---

## 💡 Resumption Sequence

Perform these steps when starting a session:

1.  **Index check**: if the top row of `STATUS/BASELINE/INDEX.md` says `pending` and its tag exists, replace `pending` with `git rev-parse --short=7 <tag>^{commit}`; if the tag does not exist, the previous Git block was not run, so tell the user before doing anything else.
2.  **Context Loading**: read `Handover.md` and, if it exists, `chat_session.md` (D7: `issues.md` only as far as needed).
3.  **State Verification**: verify whether the planned steps in `chat_session.md` were already partially implemented.
4.  **Issue Validation**: ensure the `## 🎯 Next Resumption Focus` in `issues.md` aligns with the session log.
5.  **Traceability Hook**: reference the specific Issue ID from the log in the first action performed.

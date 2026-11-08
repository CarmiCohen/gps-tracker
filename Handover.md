```
git add .
git commit -m "Nov8.14 - GIT TAGS ARE THE BASELINES (ISSUE-NOV8.5-O47, SIMP-NOV8.5-S1) AND NOV8.12 RELEASE INTEGRITY CLOSED (ISSUE-NOV8.13-O53)

- ISSUE-NOV8.5-O47: STATUS/BASELINE/INDEX.md lists all 90 versions with the commit of each tag (untagged: Nov8.7, Nov8.1, Nov4.37, Nov1.5); old copies frozen; no more copies (user decision, option C).
- DEVELOPER_GUIDELINES.md: Rule 9, Completion step 5 (gradle.properties) and step 10 (pending index row), session-start check that fills pending from the tag.
- TESTS: BaselineIndexTest (5 tests, chapter 157; checks the index against git; deliberate break detected); core-shared test inputs += INDEX.md and git tag refs.
- ISSUE-NOV8.13-O53 closed: tags Nov8.10 to Nov8.13 verified locally and on origin.
- New open gap ISSUE-NOV8.14-O58 (SOT rules 1.1 to 1.334 placeholder, split from O47).
- DOCS: issues.md, RESOLUTION_ARCHIVE.md, SOT (rule 1.395, 469 rules, chapter 5.101), TEST_PROCEDURE.md (147 of 157), CHAPTER_TRIAGE.md (86 of 147), Handover.md.
- VERSION: Nov8.14, versionCode 65. No app code changed."
git tag Nov8.14
git push origin main --tags
```

# Project Handover - Version Nov8.14 (Git Tags Are The Baselines)

## ▶️ Resumption State (Read First)
- **Git**: run the block above. Next session: replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov8.14^{commit}` (`BaselineIndexTest` fails until then).
- **Baselines**: a version's baseline is its tag (`git show <tag>:<path>`), listed in `STATUS/BASELINE/INDEX.md`. Do not copy documents; the old `STATUS/BASELINE/<version>/` folders are frozen.
- **Phones**: A15 (`R58X40GV2AR`) has the Nov8.13 debug builds, the Monitor is paired to `T`. Not reinstalled in Nov8.14 (no app code changed). S21FE in personal use; never test on it.
- **Test tips**: see `Handover.md` at tag Nov8.13 (`git show Nov8.13:Handover.md`): logcat tags `REV_*`, `adbd:S` without `-s`, `device_audit.cmd` from `cmd`.
- **Details of every resolved issue**: `STATUS/RESOLUTION_ARCHIVE.md`. Open issues and the queue: `DOCS/issues.md`.

## 🟢 Status: PASSED (Nov8.14)
- ISSUE-NOV8.5-O47 and SIMP-NOV8.5-S1 resolved (option C); ISSUE-NOV8.13-O53 closed after the user ran the Nov8.13 Git block.
- All unit tests pass, including `BaselineIndexTest` 5/5 (deliberate break detected). `assembleDebug` and `assembleRelease` successful.
- Not run: device audit and `ProductionReadinessAuditTest`, because no app code changed (only `gradle.properties` version, documents, and a `core-shared` test).

**Audit line**: Nov8.14: [SOT Count: 969 (Rules: 469), Open: H:1, M:2, L:15, Ideas: H:0, M:3, L:15, Testing: PASSED (Sub-items: 334), QA: 86]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row (after the Git block).
2. ISSUE-NOV5.2-O11 Group 4 (Microphone) or Group 5 (Lift), High; the user picks which first.
3. ISSUE-NOV8.13-O55 (Medium): Perfetto trace of the Monitor start.
4. Optional, discussed 2026-11-08: lean documentation (one home per fact, shorter `issues.md`, shorter Completion Sequence); needs a user decision before any rule changes.

**Handover complete. System state locked at Nov8.14.**

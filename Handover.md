```
git add .
git commit -m "Nov8.100-Documentation - LEAN DOCUMENTATION WORKFLOW (DOCUMENTATION RULES D1 TO D7)

- DEVELOPER_GUIDELINES.md: Documentation Rules D1 to D7 (one home per fact, issues.md as working file, short Handover and commit message, tags as baselines, 10-step Completion Sequence, milestone session log, targeted reads); Rule 6 now a progress log.
- issues.md rewritten as a working file (450 to about 90 lines); old dashboard rows moved to the archive's Dashboard History; Open and Ideas recounted.
- TESTS: TrackingDocumentIntegrityTest rewritten (limits, suffixed version names, chapter 158); BaselineIndexTest and VersionSourceTest accept suffixed names.
- DOCS: RESOLUTION_ARCHIVE.md, SOT rule 1.396, TEST_PROCEDURE.md (148 of 158), CHAPTER_TRIAGE.md, INDEX.md, Handover.md.
- VERSION: Nov8.100-Documentation, versionCode 66. No app code changed."
git tag Nov8.100-Documentation
git push origin main --tags
```

# Project Handover - Version Nov8.100-Documentation (Lean Documentation Workflow)

## ▶️ Resumption State (Read First)
- **Git**: run the block above; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` (Resumption step 1).
- **Workflow**: follow `DEVELOPER_GUIDELINES.md`, now with Documentation Rules D1 to D7 and a 10-step Completion Sequence; tests enforce the limits.
- **Where things are**: resolved issues in `STATUS/RESOLUTION_ARCHIVE.md`; open work in `DOCS/issues.md`; old text in tags (`git show Nov8.14:DOCS/issues.md`).
- **Phones**: A15 (`R58X40GV2AR`) has the Nov8.13 debug builds, Monitor paired to `T`. S21FE in personal use; never test on it.
- **Test tips**: logcat tags `REV_*`, read with `adb logcat -d adbd:S` (not `-s`); `TESTS/device_audit.cmd` from `cmd`, one app per call; instrumented runs need `$env:ANDROID_SERIAL="R58X40GV2AR"` and uninstall the Sensor.

## 🟢 Status: PASSED (Nov8.100-Documentation)
- The 7 documentation points accepted on 2026-11-08 are now rules D1 to D7, enforced by `TrackingDocumentIntegrityTest` (chapter 158) and `BaselineIndexTest`. Details: `RESOLUTION_ARCHIVE.md`.
- All unit tests and `assembleDebug` / `assembleRelease` pass. Device audit not run: no app code changed.

**Audit line**: Nov8.100-Documentation: [SOT Count: 970 (Rules: 470), Open: H:1, M:3, L:8, Ideas: H:0, M:0, L:16, Testing: PASSED (Sub-items: 336), QA: 87]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row (after the Git block).
2. ISSUE-NOV5.2-O11 Group 4 (Microphone) or Group 5 (Lift), High; the user picks which first.
3. ISSUE-NOV8.13-O55 (Medium): Perfetto trace of the Monitor start.

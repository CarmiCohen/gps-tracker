```
git add .
git commit -m "Nov8.101-Tooling - DEBUG-ONLY ADB TEST COMMANDS AND ONE-LINE TEST TOOLING (SIMP-NOV8.100-S1)

- core-shared DebugCommand (strict parsing); src/debug DebugCommandReceiver in both apps (DUMP-guarded, one REV_DEBUG_CMD line): Sensor SET_ID/STATE, Monitor PAIR/UNPAIR/ADD_HOME/CLEAR_HOME/STATE.
- TESTS/run_tests.ps1, device_check.ps1, audit_line.ps1: one summary line each; usage in TEST_GUIDELINES.md section 4; Completion steps 1 and 5 use them.
- Audit line: QA read from the CHAPTER_TRIAGE summary; Sub-items now = numbered TEST_PROCEDURE sub-items.
- New gap ISSUE-NOV8.101-O59 (Sensor without a first screen has no ID). Housekeeping: CLAUDE.md token economy, D5 docs-only versions, D7.
- TESTS: DebugCommandTest (chapter 159); all unit tests, assembleDebug/assembleRelease pass; A15 commands and device audit run.
- VERSION: Nov8.101-Tooling, versionCode 67."
git tag Nov8.101-Tooling
git push origin main --tags
```

# Project Handover - Version Nov8.101-Tooling (Debug Test Commands and One-Line Tooling)

## ▶️ Resumption State (Read First)
- **Git**: run the block above; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` (Resumption step 1).
- **Workflow**: `DEVELOPER_GUIDELINES.md` (D1 to D7, 10-step Completion Sequence). New tools in `TESTS/TEST_GUIDELINES.md` section 4: `run_tests.ps1`, `device_check.ps1`, `audit_line.ps1`.
- **Where things are**: resolved issues in `STATUS/RESOLUTION_ARCHIVE.md`; open work in `DOCS/issues.md`; old text in tags.
- **Phones**: A15 (`R58X40GV2AR`) has the Nov8.101-Tooling debug builds; Monitor paired to `T` with its 2 Home Points (restored after the tests); Sensor ID set to `T` by `SET_ID` (it was `PENDING_IDENTITY`, ISSUE-NOV8.101-O59). S21FE in personal use; never test on it.
- **Test tips**: `device_check.ps1 -App monitor -Command STATE` prints pairing and Home Points before a test, to restore them after. Instrumented runs need `$env:ANDROID_SERIAL="R58X40GV2AR"` and uninstall the Sensor.

## 🟢 Status: PASSED (Nov8.101-Tooling)
- SIMP-NOV8.100-S1 done: debug-only adb commands answer in one `REV_DEBUG_CMD` line; release manifests contain no receiver. Details: `RESOLUTION_ARCHIVE.md`.
- All unit tests (366) and `assembleDebug` / `assembleRelease` pass; only the known Gradle 9 deprecations (ISSUE-NOV8.13-O57). Device audit: Sensor clean; Monitor 2 Davey frames, the known ISSUE-NOV8.13-O55. `ProductionReadinessAuditTest` not run: `src/main` unchanged, and it uninstalls the Sensor.

**Audit line**: Nov8.101-Tooling: [SOT Count: 971 (Rules: 471), Open: H:1, M:3, L:9, Ideas: H:0, M:0, L:16, Testing: PASSED (Sub-items: 296), QA: 88]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row (after the Git block).
2. SIMP-NOV8.100-S2 (item 50): documentation cleanup.
3. ISSUE-NOV5.2-O11 Group 4 (Microphone) or Group 5 (Lift), High; the user picks which first.

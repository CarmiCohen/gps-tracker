# JD project - standing session instructions

These apply to every session automatically; the user does not need to repeat them.

- **Start**: run the Resumption Sequence of `DEVELOPER_GUIDELINES.md`: read `Handover.md` and `chat_session.md` if it exists, then take the next item of the Next Resumption Focus in `DOCS/issues.md` unless the user names another. Display the chosen issue before fixing it (Rule 1).
- **Rules**: follow `DEVELOPER_GUIDELINES.md`, including Documentation Rules D1 to D7 and the 6-step Completion Sequence. The archive, SOT, baseline index, dashboard and audit line are frozen at Nov23.4: never write to them (user decision 2026-11-23).
- **Git**: at Completion step 5 Claude commits, tags and pushes the main repo itself, without asking (user decision 2026-11-19); the commit message is the version's record (D1); never force-push.
- **Session log (D6)**: keep `chat_session.md` (git-ignored) as a short, append-only milestone log from the first action on: decisions, test results, next steps, temporary device state to restore (changed files are logged automatically by the hook in `.claude/settings.json`). Delete it at Completion step 5.
- **Version name**: use the name the user gives; otherwise ask once.
- **Token economy**: one Completion Sequence per session; small related items may share it (user decision 2026-11-23). After the Final stop, suggest `/clear`. Only the user can change `/effort` or `/model`: recommend it once, when displaying the chosen issue, then carry on without stopping. **`/effort low`**: bookkeeping, documentation-only versions, simple device checks, running tests. **`/effort high`**: new features or tricky fixes (new sensor logic, thresholds, concurrency, Android platform rules, performance traces). Otherwise medium. Keep progress notes to one short line.
- **Phones**: test on the A15 (`R58X40GV2AR`); the S21FE is in personal use. When both are connected, use the Two-phone test of `DOCS/issues.md` Operational Notes (Sensor on the A15, Monitor on the S21FE) without asking the user (user decision 2026-11-17, Nov17.4); still check the S21FE's foreground app first.

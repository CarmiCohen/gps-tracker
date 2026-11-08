# JD project - standing session instructions

These apply to every session automatically; the user does not need to repeat them.

- **Start**: run the Resumption Sequence of `DEVELOPER_GUIDELINES.md` (index `pending` row check first), read `Handover.md` and `chat_session.md` if it exists, then take the next item of the Work Queue in `DOCS/issues.md` unless the user names another. Display the chosen issue before fixing it (Rule 1).
- **Rules**: follow `DEVELOPER_GUIDELINES.md` strictly, including Documentation Rules D1 to D7 and the 10-step Completion Sequence.
- **Session log (D6)**: keep `chat_session.md` (git-ignored) as a short, append-only milestone log from the first action on: decisions, test results, next steps, temporary device state to restore (changed files are logged automatically by the hook in `.claude/settings.json`). Enough to resume after a crash; no long rewrites. Delete it at Completion step 9.
- **Version name**: use the name the user gives; otherwise ask once.
- **Token economy**: one version per session; after the Final stop, suggest starting a new session. Routine documentation work does not need the strongest model or highest effort; suggest `/model` or `/effort` to the user when a session is only bookkeeping.
- **Phones**: test only on the A15 (`R58X40GV2AR`); the S21FE is in personal use.

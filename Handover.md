```
git add .
git commit -m "Nov10.7 - TWO PHONES, ONE LATE VIEWER (ISSUE-NOV5.0-O4)

- ISSUE-NOV5.0-O4: chapters run with the S21FE Monitor and the A15 Sensor (production relay).
  125.7 passed (64 s cut, no new hole); 137.4 passed (unanswered request asked again, no restart).
  19.1 failed: second viewer 11.2 s and 17.0 s late on two of three alarms.
- New: ISSUE-NOV10.7-O73 (late viewer, Medium), O74 (Alerts close button), O75 (empty-list siren); SOT 1.416.
- ISSUE-NOV5.2-O16: stale rows stretch every backfill request.
- Index row Nov10.6-Documentation filled (fb29469).
- TESTS: :core-shared:test (docs-only version, D5).
- VERSION: Nov10.7, versionCode 86."
git tag Nov10.7
git push origin main --tags
```

# Project Handover - Version Nov10.7 (Two Phones, One Late Viewer)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.7^{commit}`.
- **User decisions (2026-11-10)**: items 53 (O61) and 62 (O71) are very low priority. `//` from the user means "continue". The S21FE was allowed for the Nov10.7 session only, with nothing changed except its Monitor app; ask again before each use.
- **Phones**: A15 (`R58X40GV2AR`): Sensor `T` running, debug Monitor `V` paired to `T`, both Nov10.3 debug builds; mobile data on. S21FE (`R5CRC14PG4F`): release Monitor Nov10.3 paired to `T`; its noise alarm switch was turned on by the user.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4. A network cut on cue: a PC loop watching the Monitor logcat file for `Requested`, then `adb shell svc data disable` / `enable` on the A15.

## 🟢 Status: PASSED (Nov10.7)
- ISSUE-NOV5.0-O4 reduced: 125.7 and 137.4 passed on two phones; chapter 19 failed (ISSUE-NOV10.7-O73). Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `:core-shared:test` passed. Docs-only version (D5): no builds, no device audit.

**Audit line**: Nov10.7: [SOT Count: 990 (Rules: 490), Open: H:0, M:3, L:10, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 333), QA: 112]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 63, ISSUE-NOV10.7-O73: find where the second viewer's 11 to 17 s delay arises (`/effort high`; needs the S21FE with the user's permission).

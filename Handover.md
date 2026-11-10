```
git add .
git commit -m "Nov10.9-moreIdeas - MORE IDEAS LOGGED (ISSUE-NOV10.8-O80 to O86, O76)

- Documentation only; no issue resolved.
- New: O80 setup survives reinstall, O81 Sensor-specific Phone Setup, O82 dead Sensor siren page, O83 Sensor set up once / Monitor controls it.
- New: O84 revive Events, Info, Graphs; O85 per-point home delete; O86 alarms by push (very low).
- O76 widened to all dead menu buttons in both apps, with user decisions.
- Index row Nov10.8 filled (905d1d9).
- TESTS: :core-shared:test.
- VERSION: Nov10.9-moreIdeas, versionCode 88."
git tag Nov10.9-moreIdeas
git push origin main --tags
```

# Project Handover - Version Nov10.9-moreIdeas (More Ideas Logged)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.9-moreIdeas^{commit}`.
- **User decisions (2026-11-10)**: recorded in `DOCS/issues.md` under O76 and O80 to O86 (O86 very low priority); items 53 (O61) and 62 (O71) very low priority. `//` from the user means "continue". Ask before each use of the S21FE.
- **Phones**: unchanged since Nov10.8 (no device work this version). A15 (`R58X40GV2AR`): Nov10.8 debug builds, Sensor `T` running, Monitor `V` paired to `T`, no Home Points. S21FE (`R5CRC14PG4F`): untouched, release Monitor Nov10.3.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov10.9-moreIdeas)
- Documentation only: user requests logged as ISSUE-NOV10.8-O80 to O86; O76 widened with decisions. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `:core-shared:test` only (D5); no builds or device audit, as no code changed.

**Audit line**: Nov10.9-moreIdeas: [SOT Count: 991 (Rules: 491), Open: H:0, M:7, L:14, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 336), QA: 113]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 63, ISSUE-NOV10.7-O73: the second viewer's late alarms (`/effort high`; needs the S21FE with the user's permission).
3. Item 64, ISSUE-NOV10.8-O76: implement the decided menu buttons (`/effort medium`).
4. Then O82 and O81, O83, O84 (three versions), O85, O80; O86 very low priority.

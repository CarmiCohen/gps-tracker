```
git add .
git commit -m "Nov10.5-Documentation - THE SOT HOLDS ITS RULES (ISSUE-NOV8.14-O58)

- O58: SOT rules 1.304-1.340 restored verbatim from git history; placeholder replaced by a note:
  1.1-1.294 live only in the pre-split repo history (5e1d65a9), 1.295-1.303 were never committed.
- TrackingDocumentIntegrityTest: rules 1.304..newest must form one unbroken run. SOT 1.414, chapter 176.
- New idea SIMP-NOV10.5-S1 (stale restored rules). Index row Nov10.4 filled (8496e65).
- TESTS: :core-shared:test (docs-only version, D5).
- VERSION: Nov10.5-Documentation, versionCode 84."
git tag Nov10.5-Documentation
git push origin main --tags
```

# Project Handover - Version Nov10.5-Documentation (The SOT Holds Its Rules)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.5-Documentation^{commit}`.
- **User decisions (2026-11-10)**: items 53 (O61) and 62 (O71) are very low priority. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`): untouched this version; Sensor and Monitor debug builds (versionCode 82), Sensor data, pairing and permissions reset since Nov10.4: re-pair and re-grant before device tests. S21FE in personal use; never test on it.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov10.5-Documentation)
- ISSUE-NOV8.14-O58 resolved: 37 lost SOT rules restored, origin of 1.1 to 1.303 recorded, guard test added. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `:core-shared:test` passed. Docs-only version (D5): no builds, no device audit.

**Audit line**: Nov10.5-Documentation: [SOT Count: 988 (Rules: 488), Open: H:0, M:2, L:8, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 332), QA: 110]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Next open gap by priority from `DOCS/issues.md` (the two Medium gaps first), or SIMP-NOV10.5-S1 (review the restored SOT rules; `/effort medium`).

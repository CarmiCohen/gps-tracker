```
git add .
git commit -m "Nov10.6-Documentation - RETIRED RULES KEEP THEIR NUMBERS (SIMP-NOV10.5-S1)

- SIMP-NOV10.5-S1: SOT rules 1.304-1.340 checked against the code; 33 hold.
  Retired in place: 1.318 (HiltAndroidRule val), 1.333 (old relay URL), 1.336, 1.339 (version milestones).
- SOT 1.415 (a superseded rule is retired in place), chapter 177.
- Index row Nov10.5-Documentation filled (e753ad1).
- TESTS: :core-shared:test (docs-only version, D5).
- VERSION: Nov10.6-Documentation, versionCode 85."
git tag Nov10.6-Documentation
git push origin main --tags
```

# Project Handover - Version Nov10.6-Documentation (Retired Rules Keep Their Numbers)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.6-Documentation^{commit}`.
- **User decisions (2026-11-10)**: items 53 (O61) and 62 (O71) are very low priority. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`): untouched since Nov10.4; Sensor and Monitor debug builds (versionCode 82), Sensor data, pairing and permissions reset: re-pair and re-grant before device tests. S21FE in personal use; never test on it.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov10.6-Documentation)
- SIMP-NOV10.5-S1 resolved: 33 of the 37 restored SOT rules hold; 1.318, 1.333, 1.336 and 1.339 are retired in place. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- `:core-shared:test` passed. Docs-only version (D5): no builds, no device audit.

**Audit line**: Nov10.6-Documentation: [SOT Count: 989 (Rules: 489), Open: H:0, M:2, L:8, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 333), QA: 111]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Both Medium gaps are blocked (O4, O26). Next workable: ISSUE-NOV10.4-O72, Kotlin Gradle plugin upgrade or kapt to KSP (`/effort high`), or a simplification idea from `DOCS/issues.md`.

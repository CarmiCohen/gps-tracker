```
git add .
git commit -m "Nov10.4 - THE BUILD MUTATES NO RESOLVED CONFIGURATION (ISSUE-NOV8.13-O57)

- O57: AGP 8.2.2 -> 8.5.2; its classpath alignment caused the Gradle 9 'Mutating' deprecations.
- CleanSetupActionsTest: redundant initializers removed. New gap O72 (kapt fileCollection).
- O61 and O71 set to very low priority. Index row Nov10.3 filled (d82ffa1). SOT 1.413, chapter 175.
- TESTS: 557 unit tests, assembleDebug/Release, A15 audit passed.
- VERSION: Nov10.4, versionCode 83."
git tag Nov10.4
git push origin main --tags
```

# Project Handover - Version Nov10.4 (The Build Mutates No Resolved Configuration)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.4^{commit}`.
- **User decisions (2026-11-10)**: upgrade AGP for O57; items 53 (O61) and 62 (O71) are very low priority. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Sensor and Monitor debug builds (versionCode 82) restored. `connectedDebugAndroidTest` uninstalled the Sensor, so its data, pairing and permissions are reset: re-pair and re-grant before device tests. S21FE in personal use; never test on it.
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24. Only deprecation left: kapt `fileCollection(Spec)` (O72).
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4.

## 🟢 Status: PASSED (Nov10.4)
- ISSUE-NOV8.13-O57 resolved: no "Mutating the dependencies of configuration" line. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- 557 unit tests, `assembleDebug`, `assembleRelease`; A15: `ProductionReadinessAuditTest` 3/3, release cold starts clean in both apps.

**Audit line**: Nov10.4: [SOT Count: 987 (Rules: 487), Open: H:0, M:2, L:9, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 331), QA: 109]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 48, ISSUE-NOV8.14-O58 (SOT rules 1.1 to 1.334 placeholder; `/effort medium`).

```
git add .
git commit -m "Nov10.3 - THE RELEASE INSTALL NAMES ITS FAILURE (ISSUE-NOV9.8-O70)

- O70: install_release.ps1 prints adb's or apksigner's own message instead of RemoteException.
- Index row Nov10.2 filled (c9cac4e). SOT 1.412, chapter 174.
- TESTS: tooling and documentation only, :core-shared:test.
- VERSION: Nov10.3, versionCode 82."
git tag Nov10.3
git push origin main --tags
```

# Project Handover - Version Nov10.3 (The Release Install Names Its Failure)

## ▶️ Resumption State (Read First)
- **Git**: run the block above as one commit, then the tag; next session, replace `pending` in the top row of `STATUS/BASELINE/INDEX.md` with `git rev-parse --short=7 Nov10.3^{commit}`.
- **User decisions (2026-11-10)**: keep the 30 dB noise jump; O69 closed. `//` from the user means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Sensor and Monitor debug builds (versionCode 79), Sensor service running; background location not "Allow all the time". Not touched in Nov10.3. S21FE in personal use; never test on it.
- **Test tips**: `DOCS/issues.md` Operational Notes; `TESTS/TEST_GUIDELINES.md` section 4. `install_release.ps1` failures now name their reason (chapter 174).

## 🟢 Status: PASSED (Nov10.3)
- ISSUE-NOV9.8-O70 resolved: `TESTS/install_release.ps1` reports the tool's own failure message. Details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Tooling and documentation only: `:core-shared:test`; no builds or device audit (D5).

**Audit line**: Nov10.3: [SOT Count: 986 (Rules: 486), Open: H:0, M:2, L:9, Ideas: H:0, M:0, L:22, Testing: PASSED (Sub-items: 330), QA: 108]

## 🎯 Next Resumption Focus
1. Fill the `pending` index row with the tag's commit (`/effort low`).
2. Item 62, ISSUE-NOV10.1-O71 (2.2 s window; `/effort high`).

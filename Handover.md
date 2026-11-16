```
git add .
git commit -m "Nov16.7 - THE WINDOW WAS THE SOUND (ISSUE-NOV10.1-O71)

- O71 closed as not a defect: the shell notification channel does not vibrate, so the 2.2 s window was the notification sound's playback, not the 3 s timer.
- Docs-only version: :core-shared:test passed; no code change.
- Version Nov16.7 (113)."
git tag Nov16.7
git push origin main --tags
```

# Project Handover - Version Nov16.7 (The Window Was The Sound)

## ▶️ Resumption State (Read First)
- **Git**: Nov16.7 waits for the Git block above; the index row Nov16.7 says `pending` (the next session fills it from the tag). Nov16.6 docs commit `9dd3a3b` is pushed.
- **Relay**: separate repo `relay-server` (`gps-survival-relay`), v6.046 with `yarn.lock` (commit `c9dd901`); live on Render with `push: on`; Render deploys only by the user's Manual Deploy.
- **PC**: Node 24.22.1; Yarn only through `corepack yarn@1.22.22`. If Kotlin compiles fail with `Daemon compilation failed: null`, run `gradlew --stop` and build with `-Pkotlin.compiler.execution.strategy=in-process` (quoted in PowerShell). `run_tests.ps1` is in `TESTS/`.
- **User decisions**: Claude pushes the relay repo; ask before each use of the S21FE and restore it after; when the effort level should change, stop and ask; `//` means "continue".
- **Phones**: A15 (`R58X40GV2AR`): Nov16.6 debug Sensor and Monitor, paired, ringer on vibrate. S21FE (`R5CRC14PG4F`): release Monitor Nov16.6 (code 112), paired to `T`, Global Mute on.
- **Test tips**: never `connectedDebugAndroidTest` (it uninstalls the app); use `am instrument`, and pin Gradle installs with `$env:ANDROID_SERIAL='R58X40GV2AR'` while the S21FE is connected (`DOCS/issues.md` Operational Notes).
- **Build**: AGP 8.5.2, Gradle 8.12, Kotlin 1.9.24; only deprecation left: kapt `fileCollection(Spec)` (O72).

## 🟢 Status: PASSED (Nov16.7)
- ISSUE-NOV10.1-O71 closed as not a defect (cause found, no code change); details: `STATUS/RESOLUTION_ARCHIVE.md`.
- Docs-only version (D5): `:core-shared:test` PASSED; no builds, no device audit.
- Not run: the live repetition with the ringer on sound (no shell ringer command on the A15); 182.2 waits for the next Manual Deploy.

**Audit line**: Nov16.7: [SOT Count: 1012 (Rules: 512), Open: H:0, M:3, L:19, Ideas: H:0, M:0, L:23, Testing: PASSED (Sub-items: 401), QA: 133]

## 🎯 Next Resumption Focus
1. Fill the index row Nov16.7 from the tag (`/effort low`).
2. At the next Manual Deploy on Render: check chapter 182.2 (`/effort low`).
3. O100 parts 1, 3 and 4, O99 parts 2 to 4, O86, O94 to O97, O98 parts 2 and 4 when convenient.

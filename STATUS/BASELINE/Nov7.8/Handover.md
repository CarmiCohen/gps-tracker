git add .
git commit -m "Nov7.8 - TEST HARDENING & CHAPTER ALIGNMENT (ISSUE-NOV6.0-O23)

- ISSUE-NOV6.0-O23: Strengthened IdentityPersistenceTest with a real manager re-instantiation / re-opening persistence test across app restarts. Updated TESTS/TEST_PROCEDURE.md and TESTS/VERIFICATION_MANIFEST.md to reference Nov7.8.
- TESTS: All automated unit and instrumentation tests passing across all subprojects (:core-shared, :core-network, :core-ui, :app-monitor, :app-sensor).
- DOCS: Updated issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md, and baseline STATUS/BASELINE/Nov7.8/.
- VERSION: Nov7.8."
git tag Nov7.8
git push origin main --tags

# Project Handover - Version Nov7.8 (Test Hardening & Chapter Alignment)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.8 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.8 commit and tag exist.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O21 (item 14): limit HUD flows to 2Hz.

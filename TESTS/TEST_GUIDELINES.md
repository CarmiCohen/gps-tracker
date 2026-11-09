# Testing Guidelines - GPS Tracker

This document establishes the standards for manual and automated testing within the project.

## 1. Manual Testing Protocol (Core Directive)
As requested by the user, the test procedure and test results must remain strictly separated:

*   **Execution**: Run the `TEST_PROCEDURE.md` as the primary protocol.
*   **Test Procedure (`TEST_PROCEDURE.md`)**: This file is the static source of truth for test protocols.
    *   **Constraint**: Keep `TEST_PROCEDURE.md` unchanged during testing.
*   **Test Results**: recorded once, in the version's section of `STATUS/RESOLUTION_ARCHIVE.md` (D1); the chapter's `**Status:**` line in `TEST_PROCEDURE.md` is updated when a result changes it. The old results log up to Nov7.3 is `ARCHIVE/test_results.md` (frozen, Nov8.102-Documentation).
*   **Issue Tracking**: Add each new identified issue to `issues.md`.
*   **Fixing Policy (Remediation Directive)**: In the event of a failure, immediate remediation MUST be performed. All findings and remediation actions MUST be formally cataloged in `issues.md` and `STATUS/RESOLUTION_ARCHIVE.md`. Upon failure detection and subsequent remediation, the testing session MUST be terminated **after updating the status files** to lock the forensic state. All documentation must strictly adhere to the **Forensic Integrity Check** (Developer Rule 8): zero placeholders, absolute completeness, and verified correctness.

## 2. Separation of Concerns
Results are not written into the procedure steps; they go to the archive, and each chapter keeps only its one-line status. This ensures that the testing workflow remains clean and the audit trail is preserved without cluttering the instructions.

## 3. Testing Constraints
*   **Logical Status**: Verifies that code follows architectural requirements and log signatures.
*   **Physical Status**: Verifies hardware resolution (e.g., A15/S21FE) and environment states.

## 4. Token-Lean Tooling (Nov8.101-Tooling, SIMP-NOV8.100-S1)
Each script prints one summary line; run them with `powershell -NoProfile -ExecutionPolicy Bypass -File TESTS/<script>`.
*   **`run_tests.ps1 [tasks...]`**: runs Gradle with `--warning-mode all` (default task `test`) and prints `TESTS PASSED|FAILED tests=... failures=...`, plus one line per failed test. The full output is in `build/run_tests.log`. `tests=up-to-date` means Gradle re-ran nothing.
*   **`audit_line.ps1 -Version <name>`**: prints the Completion step 5 audit line, computed from the SOT header, `DOCS/issues.md`, `TEST_PROCEDURE.md` (Sub-items = numbered sub-items, from Nov8.101-Tooling), and the chapter `**Status:**` lines of `TEST_PROCEDURE.md` (QA = chapters with L or A, from Nov8.102-Documentation; the script stops if a chapter has no status line).
*   **`device_check.ps1`**: A15 (`R58X40GV2AR`) by default. Sends a debug command and prints the app's one `REV_DEBUG_CMD` answer, or with `-ScreenText <text>` searches a text-only `uiautomator` dump. No screenshots.
    *   Sensor: `-App sensor -Command SET_ID -Id T`, `-Command STATE` (ID, relay, and since Nov9.0-LiftTilt `mic=`, `motionArmed=`, `barometer=`).
    *   Monitor: `-App monitor -Command PAIR -Id T`, `UNPAIR`, `ADD_HOME -Lat 32.65 -Lng 35.30`, `CLEAR_HOME`, `STATE` (prints the pairing and every Home Point, so they can be restored).
    *   The commands exist only in debug builds (`src/debug` receivers, guarded by the `DUMP` permission that only the adb shell holds). They write the same stores as the UI, so the running app reacts as to a user change.
*   **`install_release.ps1 [-Apps sensor,monitor]`** (Nov9.3-MonitorStart, ISSUE-NOV8.13-O55): after `assembleRelease`, signs each release APK with the debug key and installs it over the debug app (data and pairing kept), printing `RELEASE <app> INSTALLED|FAILED`. The device audit's frame check runs on these builds: a debug build runs in ART's interpreter (Monitor first frame 726 ms debug, about 0.45 s release on the A15). The debug commands above do not exist in them; restore the debug builds with `gradlew :app-sensor:installDebug :app-monitor:installDebug`.

---
*Established on Oct 14, 2024. Hardened on Oct 20, 2024 (Oct20.0). Updated Nov 1, 2024 (Nov1.4) to permit status updates. Updated Nov 1, 2024 (Nov1.8) to mandate immediate remediation prior to termination.*

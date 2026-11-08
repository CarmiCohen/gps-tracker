# Testing Guidelines - GPS Tracker

This document establishes the standards for manual and automated testing within the project.

## 1. Manual Testing Protocol (Core Directive)
As requested by the user, the test procedure and test results must remain strictly separated:

*   **Execution**: Run the `TEST_PROCEDURE.md` as the primary protocol.
*   **Test Procedure (`TEST_PROCEDURE.md`)**: This file is the static source of truth for test protocols.
    *   **Constraint**: Keep `TEST_PROCEDURE.md` unchanged during testing.
*   **Test Results (`test_results.md`)**: This file is the dynamic record for tracking outcomes.
    *   **Action**: Log the test results onto `test_results.md`. On future tests, just update this file.
*   **Issue Tracking**: Add each new identified issue to `issues.md`.
*   **Fixing Policy (Remediation Directive)**: In the event of a failure, immediate remediation MUST be performed. All findings and remediation actions MUST be formally cataloged in `issues.md` and `test_results.md`. Upon failure detection and subsequent remediation, the testing session MUST be terminated **after updating the status files** to lock the forensic state. All documentation must strictly adhere to the **Forensic Integrity Check** (Developer Rule 8): zero placeholders, absolute completeness, and verified correctness.

## 2. Separation of Concerns
All historical results have been removed from the procedure document and migrated to the results document. This ensures that the testing workflow remains clean and the audit trail is preserved without cluttering the instructions.

## 3. Testing Constraints
*   **Logical Status**: Verifies that code follows architectural requirements and log signatures.
*   **Physical Status**: Verifies hardware resolution (e.g., A15/S21FE) and environment states.

## 4. Token-Lean Tooling (Nov8.101-Tooling, SIMP-NOV8.100-S1)
Each script prints one summary line; run them with `powershell -NoProfile -ExecutionPolicy Bypass -File TESTS/<script>`.
*   **`run_tests.ps1 [tasks...]`**: runs Gradle with `--warning-mode all` (default task `test`) and prints `TESTS PASSED|FAILED tests=... failures=...`, plus one line per failed test. The full output is in `build/run_tests.log`. `tests=up-to-date` means Gradle re-ran nothing.
*   **`audit_line.ps1 -Version <name>`**: prints the Completion step 5 audit line, computed from the SOT header, `DOCS/issues.md`, `TEST_PROCEDURE.md` (Sub-items = numbered sub-items, from Nov8.101-Tooling), and the `CHAPTER_TRIAGE.md` summary (QA = verified chapters).
*   **`device_check.ps1`**: A15 (`R58X40GV2AR`) by default. Sends a debug command and prints the app's one `REV_DEBUG_CMD` answer, or with `-ScreenText <text>` searches a text-only `uiautomator` dump. No screenshots.
    *   Sensor: `-App sensor -Command SET_ID -Id T`, `-Command STATE`.
    *   Monitor: `-App monitor -Command PAIR -Id T`, `UNPAIR`, `ADD_HOME -Lat 32.65 -Lng 35.30`, `CLEAR_HOME`, `STATE` (prints the pairing and every Home Point, so they can be restored).
    *   The commands exist only in debug builds (`src/debug` receivers, guarded by the `DUMP` permission that only the adb shell holds). They write the same stores as the UI, so the running app reacts as to a user change.

---
*Established on Oct 14, 2024. Hardened on Oct 20, 2024 (Oct20.0). Updated Nov 1, 2024 (Nov1.4) to permit status updates. Updated Nov 1, 2024 (Nov1.8) to mandate immediate remediation prior to termination.*

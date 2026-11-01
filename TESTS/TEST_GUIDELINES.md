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
*   **Fixing Policy (Non-Intervention Directive)**: Immediate remediation of identified defects during active testing is strictly prohibited. All findings MUST be formally cataloged in `issues.md` and `test_results.md`. Upon failure detection, the testing session MUST be terminated immediately to lock the forensic state. All documentation must strictly adhere to the **Forensic Integrity Check** (Developer Rule 8): zero placeholders, absolute completeness, and verified correctness.

## 2. Separation of Concerns
All historical results have been removed from the procedure document and migrated to the results document. This ensures that the testing workflow remains clean and the audit trail is preserved without cluttering the instructions.

## 3. Testing Constraints
*   **Logical Status**: Verifies that code follows architectural requirements and log signatures.
*   **Physical Status**: Verifies hardware resolution (e.g., A15/S21FE) and environment states.

---
*Established on Oct 14, 2024. Hardened on Oct 20, 2024 (Oct20.0) to enforce strict forensic termination and Rule 8 integrity.*

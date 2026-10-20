# Issue #1424: Back/Return Button Termination Bug

## 🎯 Objective
Resolve the critical failure where tapping the system "return" (back) button causes the application to cease operations and close unexpectedly.

## 🚩 Problem Statement
Users report that interacting with the standard Android back/return button leads to an immediate app termination rather than expected navigation or backgrounding. This interrupts forensic tracking and telemetry streaming.

## 🛠️ Proposed Mediation
*   **BackPress Handler**: Audit `BackHandler` (Compose) or `onBackPressed` (Legacy) implementations to ensure they don't trigger `finish()` or process death.
*   **Navigation Guard**: Implement a confirmation dialog or ensure the back stack is correctly managed to prevent accidental exit from the main tracking/viewing screens.

## 📊 Requirements
*   **R-ID 601**: The application MUST NOT terminate upon a single back-press while in active tracking or viewing modes.
*   **R-ID 602**: Forensic services MUST remain active in the foreground even if the UI is dismissed via the back button.

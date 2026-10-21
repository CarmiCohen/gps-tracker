# SOT Master Requirements & Hardening Status (Oct21.04)

## 🏗️ Architectural Master Rules (314 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.234 Version String Alignment (R-ID 603)**: The version string box MUST be positioned at the bottom-most available coordinate of the UI container. (Oct21.00 - ISSUE-OCT20.32-F2).
*   **1.241 Debug Artifact Suppression (R-ID 610)**: All diagnostic status labels (e.g., `UI_READY`) MUST be wrapped in `BuildConfig.DEBUG` checks to ensure zero-visibility in production (PLATINUM) builds. (Oct21.03 - ISSUE-OCT20.33-F5).

## 🛡️ Core Hardening Baseline
*   **SOT ID 799**: UI Overlap Resolution - PASSED. Explicitly anchored `ScaleBarOverlay` to bottom-right corner in `MapComponents.kt` to resolve layout conflict with version tag. (Oct21.04 - ISSUE-OCT20.33-F4).
*   **SOT ID 798**: Debug Artifact Suppression - PASSED. Wrapped `UI_READY` label in `BuildConfig.DEBUG` in `MainAppContent.kt`. (Oct21.03 - ISSUE-OCT20.33-F5).
*   **SOT ID 790**: Version String Alignment - PASSED. Realigned version tag to `BottomStart` with minimal padding. (Oct21.00 - ISSUE-OCT20.32-F2).

---

## ✅ Verification Chapters
*   **Chapter 1.3 (UI Hardening Audit)**: PASSED - Verified corner separation between Scale Bar and Version tag. (Oct21.04).
*   **Chapter 1.2 (Debug Artifacts Audit)**: PASSED - Verified suppression of diagnostic labels in non-debug builds. (Oct21.03).
*   **Chapter 0.5 (UI Occlusion Audit)**: PASSED - Verified HUD scrim contrast and version alignment. (Oct21.00).

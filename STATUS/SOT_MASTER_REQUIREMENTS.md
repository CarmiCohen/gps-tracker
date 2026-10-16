# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (281 Rules)

### 1. Lifecycle & Resource Management
*   **1.224 Role Transition Grace Period (R-ID 455)**: A mandatory 30-second hardware boot/initialization grace period MUST be enforced in `AppAlarmManager.shouldPlaySiren()` and service-level timers MUST be reset during role transitions to prevent immediate acoustic bursts or false-positive offline alarms before the engine settles. (Oct14.1 - ISSUE-OCT16.8-F2).
*   **1.225 Unified Dialog Management (R-ID 512)**: All transient system dialogs (Termination, Disclosure, Sanitization) MUST be managed via a centralized `DialogHost` component to ensure consistent Z-axis ordering and prevent occlusion by full-screen navigation overlays. (Oct14.1 - SIMP-OCT16.8-S1).
*   **1.226 Sentinel Priority (R-ID 582)**: The `AlarmOverlay` MUST be rendered within a full-screen `Dialog` window layer to ensure absolute priority over all other UI components and navigation surfaces, maintaining its status as a critical security sentinel. (Oct14.1 - ISSUE-OCT16.8-F4).
*   **1.227 Map Background Persistence (R-ID 588)**: The `AppMapContainer` MUST remain active in the composition tree even when overlays are open to ensure background visibility (alpha 0.4) as per forensic legibility requirements, while the Dashboard is suppressed to prevent control overlap. (Oct14.1 - ISSUE-OCT16.8-F3).
*   **1.222 Global Test State Management (R-ID 748)**: Global `AtomicBoolean` states and caches (e.g., `isOsmReady`, `ShadowCache`) MUST be explicitly reset or pre-warmed via standardized methods in the Application class (e.g., `resetTestStates()`) during test initialization to ensure isolation and prevent timeout stalls across diverse hardware. (Oct14.1 - SIMP-OCT16.3-S1).
*   **1.223 Automated Hardening Verification (R-ID 749)**: All mission-critical hardening baselines and stress tests (DataStore, Forensic, Heat Mitigation) MUST be verified as part of the release block to ensure zero regression in sentinel logic. (Oct14.1).

... (Rest of rules 1.207 to 1.221 continue below)

## 🛡️ Core Hardening Baseline
*   **SOT ID 750**: Chapter 4 Audit - PASSED. Verified Viewer Mode synchronization, Remote HUD sync, and Alarm Authority. Implemented Z-Index hardening and HUD occlusion fixes. (Oct14.1).
*   **SOT ID 749**: Project-Wide Test Audit - PASSED. Verified all unit, hardening, and stress tests on physical A15 hardware for Oct14.1 release. Confirmed 100% pass rate. (Oct14.1).

---

## ✅ Verification Chapters
*   **Chapter 4 (Viewer Mode & Remote Sync)**: PASSED - Verified real-time synchronization, remote alarm authority, and temporal authority deltas. Hardened UI layering and role transition grace periods. (Oct14.1).
*   **Chapter 3 (Tracker Mode Operation)**: PASSED - Manual procedure verification completed on physical A15 hardware for Oct14.1 baseline. Verified stealth sentinel, service persistence, and HUD telemetry logic. (Oct14.1).

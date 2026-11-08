# Retired Test Chapters

These chapters were removed from `TEST_PROCEDURE.md` because they no longer apply to the project. They were code N in the former `CHAPTER_TRIAGE.md` (merged into the `TEST_PROCEDURE.md` status lines in Nov8.102-Documentation). The original wording is kept here for traceability.

Chapter numbers are not reused, so older references in `issues.md`, `ARCHIVE/test_results.md`, and baselines stay valid.

| Chapter | Reason |
| :--- | :--- |
| 2.1, 2.2 | The mode choice ended with the app split; each app has one role |
| 10, 47, 80, 94 | The project has no native code, so there is no JNI bridge to test |
| 71, 92 | Default IDs are fixed (`T` and `V`), and there is no alias field |
| 78, 84 | Sensor and Monitor are separate apps, so roles cannot mix or share restoration logic |
| 89 | One-time cleanup check for legacy ViewModels from before the split |
| 117 | Record of the Nov4.44 release procedure, not a repeatable test |
| 8.3, 26.2, 26.3 | Superseded in Nov7.3 by the user decision of 2026-11-07: the siren sounds until it is acknowledged, with no 30-second auto-stop or mute lockout (chapter 129). Chapter 8.2 lost the same `30s Siren` wording |

---

## Chapter 2 - Setup and Configuration (Bypass Active)
Chapter 2.3 remains active in `TEST_PROCEDURE.md`.
*   **2.1 Enter Tracker Mode:** Tap "Tracker".
*   **2.2 Verify Setup Bypass:** Confirm app lands directly on Dashboard (R-ID 615).

## Chapter 8 - Alarm & Siren Logic (Authority Audit)
Chapters 8.1 and 8.2 remain active in `TEST_PROCEDURE.md`. Until Nov7.2, 8.2 read: "Confirm Viewer triggers full-screen Red Alert and 30s Siren."
*   **8.3 Mute Lockout:** Dismiss siren on Viewer. Confirm 30s manual mute lockout.

## Chapter 26 - Alarm Suppression & Cooldowns
Chapter 26.1 remains active in `TEST_PROCEDURE.md`.
*   **26.2 Siren Auto-Stop:** Trigger siren; verify it stops after 30s.
*   **26.3 Lockout Period:** Dismiss siren; verify 30s manual mute lockout.

## Chapter 10 - Native Resource Lifecycle (Issue #249)
*   **10.1 Bridge Disposal:** Switch Tracker/Viewer modes 5 times. Monitor for `BaseEventQueue.dispose` failures.

## Chapter 47 - JNI Thread Affinity & Block Audit (R32)
*   **47.1 Non-blocking Audit:** Monitor native calls; verify they never block the Main UI loop.

## Chapter 71 - Tracker ID Entropy Audit
*   **71.1 Security Audit:** Verify ID randomness across 100 generations.

## Chapter 78 - Service Role Isolation (R975)
*   **78.1 Mutual Exclusivity:** Attempt to start Tracker and Viewer services simultaneously; verify rejection.

## Chapter 80 - Snap-Isolation Throttling (R312)
*   **80.1 100Hz Parity:** Audit JNI bridge for telemetry parity under 100Hz saturation.

## Chapter 84 - Initialization Unification (R491)
*   **84.1 Role-agnostic Restoration:** Verify state restoration logic is identical for both roles.

## Chapter 89 - ViewModel Decommissioning Audit (R496)
*   **89.1 Legacy Cleanup:** Confirm absolute removal of legacy feature ViewModels.

## Chapter 92 - Identity Collision Prevention (R499)
*   **92.1 Unique Generation:** Verify uniqueness in alias-aware ID generation logic.

## Chapter 94 - JNI Alignment Drift Protection (Issue #16.9)
*   **94.1 Stress Audit:** Monitor JNI bridge for 8-byte boundary alignment under 100% CPU lock.

## Chapter 117 - Nov4.44 Release Candidate Verification & Reciprocal Virtual Presence Simulation (Nov4.44)
*   **117.1 Automated Unit & Hardening Test Execution:** Verify execution of all project unit tests, hardening procedures, and stress evaluations (`test --warning-mode all`) across `:app-monitor`, `:app-sensor`, `:core-network`, `:core-shared`, and `:core-ui` with 100% passing results (0 failures).
*   **117.2 Reciprocal Virtual Presence Simulation Audit:** Verify reciprocal virtual presence testing in single-device environment: simulate a viewer virtual presence to validate tracker behavior, followed by a tracker virtual presence to validate viewer behavior.
*   **117.3 SOT Parity & Baseline Lock:** Verify parity with all 424 SOT Master Rules and lock baseline state in `STATUS/BASELINE/Nov4.44/`.

# Project Issues & Hardening Tracking (Sep.15.10)

## 🎯 Current Resumption Focus: A15 Power Profiling
Conducting long-term battery impact study for the unified power policy on physical hardware to ensure forensic release safety.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *(No high-priority open issues)*

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Legacy Cleanup (#1048)**:
    *   **Root-Cause Remediation**: Manually cleared the obsolete `ContextShadow.kt` file which became redundant after context shadowing automation moved into the `GpsApplication` lifecycle level. (R-ID 340).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Context Shadowing Automation (#1047)**:
    *   **Root-Cause Remediation**: Automated IPC optimization for package name lookups by overriding `getOpPackageName` directly in `GpsApplication`. Migrated all system service consumers from `@ShadowContext` to `@ApplicationContext` and removed the obsolete `ContextShadow` wrapper and its associated Dagger/Hilt qualifier. (R-ID 340).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **QA Validation: Signaling Deferral Parity (#1046)**:
    *   **Root-Cause Remediation**: Fixed a signaling inconsistency in `ViewerService` where a hardcoded `false` value for violation state caused incorrect Doze-mode deferral of critical telemetry. Synchronized logic with `TrackerService` to ensure `alarmManager.hasUnresolvedAlarms()` is correctly propagated. (R-ID 339).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Unified Power Policy Consolidation (#1045)**:
    *   **Root-Cause Remediation**: Consolidated fragmented Android 15 power-awareness logic, exponential backoff calculations, and Doze-state deferral policies into a unified `A15PowerPolicy` component. Ensured behavioral consistency across `ConnectivitySuite`, `TrackerService`, and `ViewerService`. (R-ID 339).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Forensic Signaling Pipeline Hardening - Advanced Backoff & Jitter (#1044)**:
    *   **Root-Cause Remediation**: Implemented exponential backoff with randomized jitter and PowerManager Doze awareness in `ConnectivitySuite`. Guaranteed signaling resilience and battery optimization under Android 15 power restrictions. (R-ID 338).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Continuous Loop Integrity & Android 15 Power Profile Hardening (#1043)**:
    *   **Root-Cause Remediation**: Conducted a comprehensive code audit and validation of background signaling loops, adaptive power-saving profiles, and state continuity under Android 15 power management restrictions to guarantee absolute forensic release safety. (R-ID 337).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Lifecycle-integrated Version & Doc Sync (#1042)**:
    *   **Root-Cause Remediation**: Integrated `syncDocsVersion` and `verifyVersionIntegrity` tasks into the `preBuild` lifecycle for all Android modules. Ensures forensic documentation and version type-safety are audited automatically on every build. (R-ID 336).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Signaling State Reduction & Redundant Event Pruning (#1041)**:
    *   **Root-Cause Remediation**: Simplified the sealed class hierarchy in `CommandRouter` by removing redundant `ViewerPulse` and `TransientDrop` events. Unified peer vitality detection around `ConnectivityEvent.PeerPulse` and removed legacy `ACTION_RELAY_STATUS` broadcast leftovers. (R-ID 335).

## 🟢 Recently Resolved Issues (Sep.15.10)
*   **Redundant Logic Pruning & Legacy Backfill Triggers Removal (#1040)**:
    *   **Root-Cause Remediation**: Conducted a deep audit of `ConnectivitySuite` to remove legacy backfill triggers now fully handled by the 60s periodic identity sync loop. Pruned all associated leftover variables (`lastForceJoinTs`) to simplify the application architecture. (R-ID 334).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 340 (Rules: 65, IDs: 340), Resolved: 1048, Open: 0, Testing: 0, Ideas: 16, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (Sep.15.10)*

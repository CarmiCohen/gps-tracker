# Architectural Audit Issues (v8.9.2)

This document tracks deviations from the high-assurance modular architecture defined in the project's core documentation.

## 1. FIXED Session Lifecycle Stability (R921/R926) - Resolution: Exhaustive state reset implemented in MainViewModel.
## 159. FIXED Database Schema Cleanup - Resolution: Implemented Room migration (v33) to remove 'ver' and 'vid' columns from all SQLite tables. (v8.8.35).
## 178. FIXED Forensic Parity: verticalVelocity Alignment - Resolution: Implemented full forensic parity for `verticalVelocity` across models, database, and ribbons. (v8.8.37).
## 179. FIXED RemoteHandler SIT Mapping Audit - Resolution: Verified 100% field parity for SIT forensics in `handleRemoteUpdate`, ensuring Tracker-side chair events are reconstructed in Viewer mode. (v8.8.37).

## 133. OPEN (Rank: 8) Xiaomi Background Stability Test - Task: Verify 10Hz polling (HIGH_FREQUENCY_GPS_POLLING_MS) and isXiaomiAutostartGranted effectiveness on a physical Xiaomi device to ensure background persistence.
## 180. OPEN (Rank: 7) Forensic Field Audit (RemoteHandler) - Task: Final logical verification of SIT metrics mapping (sitVz, sitDz, sitBaro, sitTilt, sitShock) in RemoteHandler to ensure zero-loss reconstruction on the Viewer.
## 181. OPEN (Rank: 6) GPS Stability Audit Verification - Task: Verify that the GPS Stability Audit suite in TrackerService.kt provides accurate reliability metrics without excessive forensic log flooding during 10Hz polling.
## 182. OPEN (Rank: 9) Global Version Synchronization (v8.9.2) - Task: Synchronize all source headers (Constants.kt, EngineConstants.kt, etc.) and documentation (REQUIREMENTS_SOT.md, issues.md) to the v8.9.2 branding baseline to resolve current component desync.
## 183. OPEN (Rank: 5) Legacy Branding Cleanup - Task: Remove redundant icon XMLs (z.xml, z2.xml, splash.xml, etc.) from res/mipmap-anydpi-v26 to fully satisfy R935 requirements.
## 184. OPEN (Rank: 5) Muzzle Window Validation - Task: Validate if MUZZLE_WINDOW_DURATION_MS (500ms) is sufficient for safety-flushing on slower storage devices to prevent false tamper triggers.
## 185. OPEN (Rank: 4) ViewerService Listener Completion - Task: Implement missing log/trail handling in ViewerService.kt localProcessorListener to maintain architectural parity with TrackerService.
## 186. OPEN (Rank: 3) SoT Documentation Lag - Task: Update REQUIREMENTS_SOT.md status fields to reflect the verification of Issues 177-179.

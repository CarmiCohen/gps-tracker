# Project History & Versioning (July.23.04)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.23.04 (Hardening & Finality)
- **Stationary Anchor Refinement (#533)**: Implemented sliding-window coordinate averaging to stabilize urban canyon positioning.
- **Type Safety Audit (#532)**: Enforced `Double` precision across the entire kinematics and persistence pipeline (R999).
- **Acoustic Duty Cycle (#531)**: Fixed FGS notification flickering by decoupling recording state from monitoring intent.
- **Geofence Reliability (#529)**: Implemented "Accuracy Recovery" to suppress false visual jumps during GPS stabilization.
- **Persistence Hardening (#527)**: Added DataStore-backed alarm state restoration to survive OS-level service kills.
- **Power Optimization (#526)**: Implemented adaptive two-tier sensor sampling based on device movement state.
- **Forensic Audit (#525, #523)**: Consolidated forensic snapshots for atomic state evaluation and fixed mapping bugs in local history.
- **Architectural Cleanup (#528, #524)**: Decommissioned `DashboardUseCase` and decoupled UI formatting into `DashboardStateProvider`.

## July.22.11
- **Dead-Weight Purge (#513)**: Physically removed decommissioned DI artifacts, legacy feature placeholders (Video/Chat), and redundant planning documents.
- **Version Alignment**: Synchronized all authoritative files and status tracking documents to resolve the tagging conflict.

## July.22.09
- **Samsung A15 Fallback Hardening (#113)**: Upgraded Accelerometer pulse to hardware "poke" via WakeLock to prevent OS-level eviction.
- **DI Leftover Purge (#126b)**: Completed rigorous removal of legacy DI artifacts and historical references. Codebase is now 100% pure Hilt.

... [See historical logs for full records]

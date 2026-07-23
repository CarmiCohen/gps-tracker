# Status Tracking & Documentation Governance

This directory contains the authoritative records for the GPS-Tracker system's state, requirements, and history. To maintain integrity, the following rules MUST be followed:

## 1. Issue Resolution Workflow
When an issue is marked as **RESOLVED**:
1.  **Archive the Shard**: Move the corresponding file from `backlog_shards/` to `backlog_shards/archive/`.
2.  **Update the Archive**: Add the resolution summary to `RESOLUTION_ARCHIVE.md` under the current version cycle.
3.  **Sync the Counter**: Increment the "Total Unique Resolutions" count in both `issues.md` and `RESOLUTION_ARCHIVE.md`.
4.  **Prune the Active Log**: Remove the resolved entry from `issues.md`, keeping only the current cycle's primary milestones.

## 2. Source of Truth (SoT) & Manifest Sync
- Every technical requirement in `SOT_MASTER_REQUIREMENTS.md` MUST link to at least one Issue ID.
- Any change to the SoT MUST be reflected in the `VERIFICATION_MANIFEST.md` with a "Verified" date and the corresponding Issue ID.

## 3. Versioning Baseline
- The `versionName` in `app/build.gradle` is the source of truth for the current version tag (e.g., `July.23.04`).
- All status files MUST use this tag in their headers.

## 4. File Structure
- `issues.md`: Active dashboard (Open Issues + Current Cycle Resolutions).
- `RESOLUTION_ARCHIVE.md`: Unified historical record of all resolutions.
- `RELEASE_HISTORY.md`: High-level version timeline.
- `QA_VALIDATION_STATUS.md`: Pending and recently verified test tasks.
- `SOT_MASTER_REQUIREMENTS.md`: Definitive operational specification.
- `VERIFICATION_MANIFEST.md`: Formal proof of implementation for audits.

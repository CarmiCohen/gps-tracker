# Issue #027: Identity Persistence Hardening
**Status**: Resolved (v8.9.98)
**Requirement**: R974

## Description
The system faced risks of identity cross-contamination during bulk save operations, which could lead to role reversion or "split-brain" states.

## Resolution
Reinforced `MainRepository.saveSettingsBulk` with atomic uniqueness validation to ensure that Tracker and Viewer identities remain distinct and stable.

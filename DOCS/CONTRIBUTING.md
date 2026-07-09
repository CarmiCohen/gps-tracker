# Project Governance & Documentation Standard (v9.3.6)

To maintain the **Forensic Integrity** of the GPS-Tracker project, all developers must adhere to the following three-tier documentation lifecycle. This standard ensures that the project's history is auditable and its active tasks are focused.

## 1. The Three-Tier System

### 🟢 Active Workspace: `issues.md` (Root)
- **Purpose**: Primary tracking for the current phase (Hardening).
- **Rule**: Only contains Open issues, Pending validation tasks, or In-Progress technical debt.
- **Workflow**: Items are added here first. Once verified, they **must** be removed and archived.

### 🔵 Audit Archive: `STATUS/VERIFICATION_MANIFEST.md`
- **Purpose**: Formal proof of implementation and historical record.
- **Components**:
    - **Verification Manifest**: A high-level checklist of requirements and their implementation status.
    - **Resolution Archive**: Points to `STATUS/RESOLUTION_ARCHIVE.md` for the chronological history of every fixed issue.
- **Rule**: No item is "Done" until its resolution is recorded here and it is removed from the active workspace.

### 🟡 System Specification: `STATUS/SOT_MASTER_REQUIREMENTS.md` (SoT)
- **Purpose**: Definitive operational specification (Constants, Thresholds, Logic).
- **Rule**: Describes *how the system works now*. It must never contain historical "Fixed" entries. It points to `STATUS/VERIFICATION_MANIFEST.md` for the audit trail.

## 2. Definition of "Done"
A task is only considered "Done" when:
1. The code is merged and verified (on hardware if required).
2. The entry is removed from the active `issues.md`.
3. The resolution summary is appended to `STATUS/RESOLUTION_ARCHIVE.md`.
4. The **Verification Manifest** in `STATUS/VERIFICATION_MANIFEST.md` is updated to reflect the new status.

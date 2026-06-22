# Project Governance & Documentation Standard

To maintain the **Forensic Integrity** of the GPS-Tracker project, all developers must adhere to the following three-tier documentation lifecycle. This standard ensures that the project's history is auditable and its active tasks are focused.

## 1. The Three-Tier System

### 🟢 Active Workspace: `issues.md`
- **Purpose**: Primary tracking for the current phase (e.g., Hardening).
- **Rule**: Only contains Open issues, Pending validation tasks, or In-Progress technical debt.
- **Workflow**: Items are added here first. Once verified, they **must** be removed and archived.

### 🔵 Audit Archive: `COMPLIANCE.md`
- **Purpose**: Formal proof of implementation and historical record.
- **Components**:
    - **Verification Manifest**: A high-level checklist of requirements and their implementation status.
    - **Resolution Archive**: A chronological history of every fixed issue (Legacy Foundation, Middle Era, and Current Phase).
- **Rule**: No item is "Done" until its resolution is recorded here and it is removed from `issues.md`.

### 🟡 System Specification: `DOCS/REQUIREMENTS_SOT.md` (SoT)
- **Purpose**: Definitive operational specification (Constants, Thresholds, Logic).
- **Rule**: Describes *how the system works now*. It must never contain historical "Fixed" entries. It points to `COMPLIANCE.md` for the audit trail.

## 2. Definition of "Done"
A task is only considered "Done" when:
1. The code is merged and verified (on hardware if required).
2. The entry is removed from `issues.md`.
3. The resolution summary is appended to the **Resolution Archive** in `COMPLIANCE.md`.
4. The **Verification Manifest** in `COMPLIANCE.md` is updated to reflect the new status.

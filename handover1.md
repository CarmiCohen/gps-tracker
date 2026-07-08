# Forensic Handover 1: ID Mapping Synchronization (#496, #326, R400)

## 📂 Project Metadata
- **Project Name**: `gps-tracker`
- **Root Directory**: `C:/CCwork/Android Projects/gps-tracker`
- **Mapping Authority Version**: v9.3.0

## 🎯 Resolved ID Mapping State
This document serves as the terminal forensic snapshot for Requirement **R400** and its related issues. This synchronization resolves the historical collision with Issue **#496**.

### 🧩 Mapping Authority
1.  **Requirement R400 (Map Metadata Alignment)**:
    - **Issue ID**: **#400** (Authoritative).
    - **Implementation**: "UNCERTAINTY" status messages are anchored to the bottom-center of `AppMapContainer` with an 80dp vertical offset.
    - **Authority File**: `STATUS/requirements_sot.md`
2.  **Issue #326 (Intelligent Uncertainty UX)**:
    - **Scope**: Enrichment of Bayesian uncertainty states with contextual reasons (`GPS_GAP`, `JAMMER`).
    - **Status**: Verified in v9.2.2.
    - **Verification Manifest**: `STATUS/compliance.md`
3.  **Issue #496 (Archival Pointer)**:
    - **Role**: Legacy ID originally assigned to **#326**.
    - **Action**: Formally decoupled from **R400** and retired from active tracking.

### 🛠 Applied Forensic Changes
| File Path | Change Description |
| :--- | :--- |
| `app/src/main/java/com/gps19/app/MapComponents.kt` | Comments updated to reference **#400** for **R400** implementation. |
| `issues.md` | Resolution row for **R400** updated to link to **#400**. |
| `STATUS/compliance.md` | Manifest and hardening archive updated to point to **#400**. |
| `STATUS/requirements_sot.md` | Architectural baseline formalized as **R400 → #400**. |
| `STATUS/issues_archive.md` | Added resolution entry for **#400**; updated legacy table for **#496 → #326**. |
| `STATUS/issue_shards/issue_400.md` | Created new shard document for **#400**. |

## 🔍 Contextual Resumption
- Future spatial UX adjustments must use **R400 / #400**.
- Future uncertainty logic or reason-code enrichment must use **#326**.
- **Issue #496** must not be used for new work.

**TERMINAL STATE**: Document preparation complete. No further modifications made to the project.

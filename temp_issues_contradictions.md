# Issue Number (#xxx) Audit: Duplications & Contradictions

### 1. Major Functional Contradictions
*   **Issue #221**:
    *   **Contradiction**: In `issues.md` (Open Issues), it is defined as **"Acoustic 'Location Pending' Optimization"** (Bayesian Confidence Scaling).
    *   **Contradiction**: In `info-doc.md`, `RIBBONS_AND_ANALYTICAL_UI.md`, and `SETTINGS_PAGE_DETAIL.md`, it is defined as **"Battery Health / Steep Discharge"**.
*   **Issue #180**:
    *   **Contradiction**: In `compliance.md`, it is marked as **FIXED** and defined as **"Forensic Pipeline Verification"** (1:1 field mapping).
    *   **Contradiction**: In `issues.md` (Open Issues), it is defined as **"Samsung A15 Optical Proximity Limitation"** (Hardware limitation/debounce).
*   **Issue #271**:
    *   **Contradiction**: In `info-elementary-fields.md` and `compliance.md`, it refers to **"Uptime Consistency"** (uptimeMs).
    *   **Contradiction**: In `app_settings.proto`, it refers to **"Forensic Sit Metadata Persistence"**.

### 2. Duplicated Issue Numbers (Multiple Tasks)
*   **Issue #284**:
    *   **Duplicate**: Used for **"Light EMA Logic"** (rising/falling factors) in `compliance.md`, `LOCATION_SENTINEL_SPEC.md`, and `EVENTS_DOC.md`.
    *   **Duplicate**: Also used for **"Forensic Persistence Gap (Viewer)"** (Geofence state-latch) in `compliance.md`.
*   **Issue #283**:
    *   **Duplicate**: Used for **"Timing Integrity"** (Monotonic time/elapsedRealtime) in `compliance.md`, `APP_DESCRIPTION.md`, and `ARCHITECTURAL_EVOLUTION.md`.
    *   **Duplicate**: Also used for **"Forensic Persistence Gap (Tracker)"** (Stall/Tamper state-latch) in `compliance.md`.
*   **Issue #282**:
    *   **Duplicate**: Used for **"SIT Duplicate Guard"** (15s sanity check) in `compliance.md`.
    *   **Duplicate**: Used for **"Forensic Persistence Gap (Viewer)"** (Signal Loss/Jammer) in `compliance.md`.
    *   **Duplicate**: Used for **"Vibration/Tilt monitoring"** in `info-doc.md`.

### 3. Renumbering & Legacy Conflicts
*   **Issue #191 (Parameter Inconsistency)**: Linked to "Muzzle Window" and "Samsung A15", but associated time windows vary across docs: **500ms** (hysteresis), **2000ms** (muzzle suppression), and **5s** (proximity debounce).
*   **Issue #273 (Ambiguous Mapping)**: Listed as the predecessor for both **Issue #315** (Network Integrity) and **Issue #316** (Storage Integrity) in `compliance.md`.
*   **Issue #214 vs #325**: **Issue #325** replaced **Issue #214** (Authoritative Spatial Anchoring), but the legacy number **#214** is still frequently referenced in `info-elementary-fields.md` and `info-doc.md`.
*   **Issue #115 vs #322**: **Issue #322** replaced **Issue #115** (ViewModel Decoupling), but **#115** remains the primary reference in `APP_DESCRIPTION.md` and `ARCHITECTURAL_EVOLUTION.md`.

### 4. Minor Inconsistencies
*   **Issue #312**: Listed as "Documentation Gating" (Xiaomi alerts) in `compliance.md` but "Documentation Hardening" (SoT alignment) in `issues.md`.

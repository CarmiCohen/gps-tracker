# Project Issues & Hardening Tracking (Sep.06.56)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.56)
*   **Issue #934 RESOLVED: Documentation Integrity Restoration**. Restored accidentally truncated forensic requirements (R251-R267) in `STATUS/QA_VALIDATION_STATUS.md` to maintain the high-assurance audit record.

## 🟢 Recently Resolved Issues (Sep.06.55)
*   **Issue #933 RESOLVED: Viewer Forensic Parity (Audit & Revival)**. Implemented the `Stability Audit` loop and `Revival Event` observation in `ViewerService`. The Viewer role now captures high-resolution reliability percentages, GNSS jitter violations, and energy footprint verdicts (Revival delta mA/Temp), matching the forensic baseline of the Tracker role (R-ID 276).

## 🟢 Recently Resolved Issues (Sep.06.50)
*   **Issue #932 RESOLVED: HUD Synchronization**. Synchronized the HUD status bar to reflect Samsung A15 hardware adaptations. Propagated `isA15Device` from the hardware layer through `MainViewModel` and `UiStateAggregator`. Added a new `A15` badge to the `StatusBar` in `SharedUiComponents.kt` to provide forensic confirmation that background "Poke" logic and `specialUse` FGS adaptations are active (R-ID 276).

## 🟢 Recently Resolved Issues (Sep.06.45)
*   **Issue #931 RESOLVED: GPS Reception Parity (Viewer Mode)**. Remediated discrepancy where the Viewer role suffered from background GPS suppression on Samsung A15 hardware compared to Waze/Maps. Promoted `ViewerService` to include `specialUse` FGS type, implemented A15 "Poke" logic (30s hardware handshake/WakeLock), and added adaptive polling (2000ms when UI is active) to keep the GPS pipeline fresh (R-ID 276).

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)
*(No open high-priority issues)*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 50, IDs: 238), Resolved: 934, Open: 0, Testing: 90% (Sub-items: 46), Ideas: 224, QA: 262]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.56)*

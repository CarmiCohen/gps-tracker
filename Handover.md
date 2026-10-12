# Handover: Oct12.16 HUD & Collision Audit

## Current Status: YELLOW (Audit Identified Gaps)
The **Oct12.16** session performed a rigorous visual and functional audit of the Viewer Map using live snapshots from a Samsung SM-G990E. The build is stable (`:app:assembleDebug` success), but the audit revealed significant UI degradation and confirmed active engine-level stalls.

### ✅ Forensic Audit Findings

#### 1. HUD Legibility & Contrast (#ISSUE-OCT12.16-1)
- **File**: `app/src/main/java/com/gps19/app/SharedUiComponents.kt`
- **Line**: ~324 (Inside `StatusBar` composable)
- **Detail**: The `Card` container uses `alpha = 0.4f` for the surface background. Snapshot analysis shows this is insufficient against Satellite/Street map tiles.
- **Rule**: Added **R-ID 647** (Rule 1.178) to `SOT_MASTER_REQUIREMENTS.md`.

#### 2. Layout & Z-Stacking Collisions (#ISSUE-OCT12.16-2, #ISSUE-OCT12.16-3)
- **File (Version Tag)**: `app/src/main/java/com/gps19/app/MapComponents.kt` -> Line 55 (`align(Alignment.BottomEnd)`).
- **File (Scale Bar)**: `app/src/main/java/com/gps19/app/MapComponents.kt` -> Line 172 (`ScaleBarOverlay`).
- **File (Watermark)**: `app/src/main/java/com/gps19/app/SharedUiComponents.kt` -> Line ~496 (`StatusRowData`).
- **Collision**: Version tag is obscured by the scale bar. "OFFLINE" text overlaps HUD metrics when peer connectivity is lost.

#### 3. Functional Stalls (#ISSUE-OCT12.16-5)
- **Validation**: Snapshot confirms `GPS`, `TRK`, and `DAT` status are RED. Validates active stalls in `SignalingProvider` and `HardwareSuite` acquisition loops.

### 📊 Performance Metrics (Oct12.16 - Audit Baseline)
| Metric | Result | Status |
| :--- | :--- | :--- |
| **HUD Contrast Ratio** | **< 2:1 (Fail)** | 🔴 **CRITICAL** |
| **Touch Target Spacing** | **~24dp (Fail)** | 🟡 **WARNING** |
| **Engine Link Status** | **Stalled (RED)** | 🔴 **CRITICAL** |

### 🚨 Strategic Simplification Idea
- **SIMP-OCT12.16-S1: Unified HUD Overlay (High).** Merge `HeaderBar`, `GlobalStatusBar`, and map tool overlays into a single `OverlayManager` to centrally handle Z-index, transparency, and padding.

### 🔜 Resumption Path (Oct12.17)
1. **Increase HUD Opacity**: Update Line 324 in `SharedUiComponents.kt` to `alpha = 0.75f`.
2. **Shift Version Tag**: Add `40.dp` bottom padding to the version tag in `MapComponents.kt` (Line 55) to clear the scale bar.
3. **Debug Engine Stalls**: Investigate the root cause of the RED status for signaling (`DAT`) and location (`GPS`) confirmed in the Oct12.16 audit.

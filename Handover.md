# Handover: GPS Tracker Hardening (July17.08)

## 🎯 Current Status: v9.3.56 (July17.08)
The system now features a displacement-weighted anchor breakout monitor to prevent "sticky anchors" in low-accuracy environments.

## 🟢 Resolved Issues (July17.08)
1.  **Dynamic Anchor Breakout (#062 / R990)**:
    - **Problem**: "Sticky anchors" where the device remains locked to a stationary coordinate despite physical movement, often due to GPS lag or low accuracy.
    - **Root Cause**: The previous anchoring logic relied heavily on distance thresholds which were often too wide to allow for gradual breakout during slow movement.
    - **Resolution**: Implemented a displacement-weighted score monitor in `LocationProcessor`. 
        - Movement in the "transition zone" (70% of threshold) now accumulates an `anchorEscapeScore`.
        - Physical sensor motion (vibration) triggers an immediate breakout.
        - Trend analysis of consecutive points identifies sustained outward trajectories.
        - Anchors now break out reliably based on sustained physical effort rather than just single-point distance jumps.

## 🟢 Resolved Issues (July17.07)
1.  **Room Database Migration Hardening (#096)**: Resolved schema drift in the `logs` table.
2.  **Startup ANR Hardening (#096b)**: Offloaded database initialization to `Dispatchers.IO`.

## ⚠️ Known Risks & Residual Tasks
- **Anchor Sensitivity**: The `ANCHOR_DISPLACEMENT_WEIGHT` may need fine-tuning across different hardware (e.g., A15 vs high-end) if urban canyon jitter becomes too aggressive.

## 🛠️ Verification Steps
1. Engage a stationary anchor (wait for "ANCHOR LOCKED" on HUD).
2. Physically move the device or simulate sustained displacement.
3. Verify that the anchor breaks out ("ANCHOR LOCKED" disappears and logs show "Stationary Anchor breakout") before reaching the full distance threshold if motion is sustained.
4. Verify HUD shows "LOCKED" status correctly on the Dashboard and Map.

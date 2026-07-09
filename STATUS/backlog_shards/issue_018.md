# Issue #018: Stationary Anchor Hard-Lock
**Status**: Resolved (v9.2.1)
**Requirement**: R990

## Description
Stationary GPS drift ("spaghetti trails") caused erroneous distance accumulation and UI jitter when the device was indoors or stationary.

## Resolution
- Implemented coordinate clamping in `LocationProcessor.kt` using `parkingAnchorPoint`.
- Added "Behavioral Breakout" logic: releases lock immediately (0.0m threshold) if physical sensors detect motion.
- Propagated `isAnchorLocked` flag to HUD and telemetry.

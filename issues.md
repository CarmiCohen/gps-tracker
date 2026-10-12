# Project Issues & Hardening Tracking (Rigorous Audit) - Oct12.14

## 🎯 Current Resumption Focus: GPS Reception Stability & UI Threshold Harmonization.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (7)
*   **ISSUE-OCT12.13-1: GPS UI Staleness Mismatch (Screen-Off).** The UI staleness threshold (35s) is lower than the intended Screen-Off polling interval (45s).
*   **ISSUE-OCT12.13-3: GPS Hardware Lock Permanent Latch.** `GPS_HARDWARE_LOCK` is a terminal state requiring manual restart.
*   **ISSUE-OCT12.13-4: GPS Re-registration Flutter.** Frequent polling changes interrupt the GPS acquisition cycle.
*   **ISSUE-OCT12.13-6: Viewer Location Defaulting to (0,0) (Null Island).** Logic defaults Viewer coordinates to (0.0, 0.0) when no fix is available.
*   **ISSUE-OCT12.13-7: Signaling Relay & Peer Connection Failure.** Devices are failing to establish a stable telemetry link via the relay.
*   **ISSUE-OCT12.13-11: Abnormal Jitter in Stability Audit.** Logcat reveals extreme jitter (1390s) in viewer temporal tracking.
*   **ISSUE-OCT12.14-5: Live State Validation (GPS/TRK/DAT RED).** Snapshot confirms critical failures in location acquisition and data transmission, validating engine-level stalls.

### 🟡 Medium Priority (8)
*   **ISSUE-OCT12.13-2: Inconsistent Interval Authority.** `MainViewModel` freshness logic is not synchronized with `ServiceBehaviorUseCase`.
*   **ISSUE-OCT12.13-5: Outdated Dead Code (UiStateCoordinator).** Duplicated mapping logic lacks critical Oct11.7 fixes.
*   **ISSUE-OCT12.13-8: Viewer HUD Background Visual Inconsistency.** Transparency stacking causes inconsistent HUD rendering.
*   **ISSUE-OCT12.13-10: Forensic I/O Pruning Spikes.** Database pruning spikes of up to 3.4s causing UI stutter.
*   **ISSUE-OCT12.13-12: Engine Version Metadata Inconsistency.** Core components report divergent versions (Oct11.2 vs Oct12.12).
*   **ISSUE-OCT12.14-1: HUD Telemetry Illegibility.** Status metrics (V/T) are illegible against light-colored map tiles.
*   **ISSUE-OCT12.14-2: Watermark/HUD Z-Stacking Collision.** "OFFLINE" watermark overlaps with HUD metrics, causing visual clutter.
*   **ISSUE-OCT12.14-4: HeaderBar Portrait Density.** Top navigation icons have insufficient touch target spacing and size.

### 🔵 Low Priority (3)
*   **ISSUE-OCT12.13-9: Map Scale UI Overlap.** `ScaleBarOverlay` obscures the version name text.
*   **ISSUE-OCT12.14-3: HUD Error Icon Ambiguity.** Red exclamation icon at top-right lacks labels or contextual hints.
*   **ISSUE-OCT12.14-6: Navigation Inset Buffer.** Version tag lacks vertical buffer from system navigation bar.

---

## 💡 Strategic Simplification Ideas (Ideas: 12)
*   **SIMP-OCT12.14-S1: Unified HUD Overlay (High).** Merge HeaderBar, GlobalStatusBar, and map overlays into a single OverlayManager to centrally handle Z-index, transparency, and padding.
*   **SIMP-OCT12.12-S1: Pruning Deadlock Guard (Medium).** Unify pruning triggers into a shared `PersistenceWatchdog`.
*   **SIMP-OCT12.12-S2: Checksum Failure Alerting (Low).** UI indicator for forensic corruption events.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1012-4: Batch Persistence.** Implemented background batch-flush loop in `OfflineRepository.kt`. Resolved Oct12.12.
*   **Issue #SIMP-1012-3: Forensic Integrity.** Integrated CRC32 checksums into `ForensicSpillBuffer.kt`. Resolved Oct12.12.

---

## 📊 Hardening Progress Dashboard
- **Oct12.14: [SOT Count: 373 (Rules: 220), Open: H:7, M:8, L:3, Ideas: H:1, M:6, L:5]**
- **Oct12.13: [SOT Count: 369 (Rules: 219), Open: H:6, M:5, L:1, Ideas: H:0, M:6, L:5]**
- **Oct12.12: [SOT Count: 357 (Rules: 207), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:5]**

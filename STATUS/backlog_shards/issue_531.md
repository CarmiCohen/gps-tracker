# Issue #531: Acoustic Duty Cycle Logic Refinement

## Status: Resolved (July.23.04)
## Requirement: R810-L2

### Description
The Mic recording icon in the notification shade was flickering during the 8s "OFF" phases of the power-saving duty cycle, causing user confusion and potential OS-level service flags.

### Resolution
- **Intent-Based Management**: Refined `ForegroundServiceType` logic to use the *intent* to monitor (`isAcousticMonitoringEnabled`) rather than the active recording state to drive notification flags.
- **Service Lifecycle**: Decoupled the notification update frequency from the recording buffer cycle.

### Verification
- [x] Verified on Android 14 (API 34). Mic icon remains steady in the status bar while the monitor is active, regardless of the duty cycle phase.

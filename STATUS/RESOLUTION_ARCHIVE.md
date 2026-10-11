# Resolution Archive

## [Oct11.6] - Radio-Warm Keep-Alive
**Issue ID**: #SIMP-1011-21
**Status**: RESOLVED
**Description**: Implemented a periodic GNSS radio keep-alive strategy to prevent hardware residency lock on budget Samsung devices.
**Root Cause**: Samsung App Standby (Staggered Tier) forces the GNSS radio into a deep sleep state after ~8 minutes of background execution, ignoring standard recovery pulses.
**Remediation**:
- **Radio-Warm Keep-Alive (#SIMP-1011-21)**: Implemented `checkRadioResidency` in `HardwareSuite.kt`.
- **Periodic Bursts**: Added a 60s periodic keep-alive trigger using `PRIORITY_BALANCED_POWER_ACCURACY` to maintain radio residency during background or stationary periods.

## [Oct11.5] - Background Transition Hardening
**Issue ID**: #SIMP-1011-20
**Status**: RESOLVED
**Description**: Hardened foreground-to-background transitions to prevent process death on budget hardware during high sensor load.
**Root Cause**: Low-tier devices (Staggered Tier) frequently terminated background processes during the initial 60s of background transition due to resource contention.
**Remediation**:
- **Escalation Window (#SIMP-1011-20)**: Implemented a 60s priority escalation window in `DeviceHardeningStrategy.kt` to ensure stable resource allocation during the transition.

## [Oct11.5] - Signaling Role Alignment
**Issue ID**: #SIMP-1011-19
**Status**: RESOLVED
**Description**: Refined signaling validation to eliminate echo interference during high-gain sensor cycles.
**Root Cause**: Reflected pulses were being interpreted as new signaling events, causing logic stalls and incorrect geofence triggers.
**Remediation**:
- **Echo Suppression (#SIMP-1011-19)**: Implemented authoritative echo suppression in `SignalingValidator.kt` to filter reflected pulses.

## [Oct11.5] - Event Bus Backpressure Hardening
**Issue ID**: #SIMP-1011-17
**Status**: RESOLVED
**Description**: Audited and hardened the DomainEventBus to ensure critical telemetry integrity during high-frequency sensor floods.
**Root Cause**: The previous BufferOverflow.DROP_OLDEST strategy with a 512-slot buffer was susceptible to evicting CRITICAL alarms if the buffer was saturated by NORMAL/LOW priority sensor noise during UI-induced backpressure.
**Remediation**:
- **Staged Priority Gating (#SIMP-1011-17)**: Implemented priority-aware emission logic in `DomainEventBus.kt`.
- **Buffer Expansion**: Increased `DOMAIN_EVENT_BUS_CAPACITY` to 1024 in `EngineConstants.kt`.
- **Subscription Pressure Guard**: Added staged dropping (LOW events at 1x threshold, NORMAL events at 2x threshold) to guarantee buffer headroom for HIGH and CRITICAL priority events.

...

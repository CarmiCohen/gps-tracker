# Issue #1362: Forensic Persistence Health Alerting

## 🎯 Status: RESOLVED
**Category**: Hardening / Forensic
**Priority**: High
**Cycle**: Sep.28.16

---

## 📝 Description
Implement the alerting logic for forensic persistence reliability as specified in R715. The system must trigger a `PERFORMANCE_SPIKE` alert if the forensic reliability EMA drops below the critical threshold (0.85) for a sustained period (30s).

## 🛠️ Implementation
- **IntegrityMonitor.kt**: 
    - Added monitoring of `h.forensicReliability` within `performIntegrityHeartbeat`.
    - Integrated `FORENSIC_RELIABILITY_THRESHOLD` (0.85) check.
    - Utilized `sustainedViolations` map to debounce breaches against `FORENSIC_RELIABILITY_DEGRADATION_DURATION_MS` (30s).
    - Emits `IntegrityEvent.ViolationSustained(ALERT_ID_PERFORMANCE_SPIKE)` and forensic LogEvent upon sustained failure.
    - Emits `IntegrityEvent.ViolationResolved(ALERT_ID_PERFORMANCE_SPIKE)` upon recovery.

## 🔗 References
- **Requirement**: R715 (Persistence Health Alerting Authority)
- **SOT ID**: 530 (Forensic Persistence Health Alerting)

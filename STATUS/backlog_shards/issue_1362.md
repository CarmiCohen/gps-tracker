# Issue #1362: Forensic Persistence Health Alerting

## 🎯 Status: OPEN
**Category**: Hardening / Forensic
**Priority**: High

---

## 📝 Description
Implement the alerting logic for forensic persistence reliability as specified in R715. The system must trigger a `PERFORMANCE_SPIKE` alert if the forensic reliability EMA drops below the critical threshold (0.85) for a sustained period (30s).

## 🛠️ Proposed Remediation
- **IntegrityMonitor.kt**: Add a check in `performIntegrityHeartbeat` to monitor `health.forensicReliability`.
- **Logic**: Use `sustainedViolations` to track the duration of the low reliability state.
- **Alerting**: Emit `IntegrityEvent.ViolationSustained(ALERT_ID_PERFORMANCE_SPIKE)` when the 30s threshold is crossed.

## 🔗 References
- **Requirement**: R715 (Persistence Health Alerting Authority)
- **SOT ID**: 529 (Forensic Persistence Health Alerting)

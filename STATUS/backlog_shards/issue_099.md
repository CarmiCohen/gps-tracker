# Issue #099: ANR Hardening (Cold-Start Delay)

## 🎯 Status: Resolved (Historical)
**Category**: Performance / Startup

---

## 📝 Description
Budget hardware was experiencing Application Not Responding (ANR) errors during cold start due to simultaneous initialization of high-frequency GPS listeners and repository pruning.

## 🛠️ Resolution
- Implemented a mandatory 500ms staggered delay before starting base observations in `MainActivity.kt`.
- Offloaded non-essential hardware property checks to the `MainViewModel`.
- Established the **Cold-Start Hardening (R955b)** requirement to gate logic pulses during the first second of application life.

## 🔗 References
- **Requirement**: R955b (Cold-Start Hardening)
- **File**: `app/src/main/java/com/gps19/app/MainActivity.kt`, `SOT_MASTER_REQUIREMENTS.md`

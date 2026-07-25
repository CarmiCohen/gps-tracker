# Issue #543: Missing Native Library Dependency (`libmbrainSDK`)

## 🎯 Status: Open (July.24.06)
**Category**: Hardware Integration / Compatibility

---

## 📝 Description
The system is failing to load the vendor-specific `libmbrainSDK` native library on target hardware (specifically MediaTek-based Samsung devices). Logcat shows `initMbrain failed`.

## 🔍 Observations
- **Error**: `initMbrain failed` and library load errors in Logcat.
- **Impact**: Loss of vendor-specific hardware optimizations and "pokes" for MediaTek/Samsung chipsets, potentially affecting stay-alive performance.

## 🛠️ Planned Action
- Locate the correct `.so` files for the target architectures.
- Verify JNI bridge signatures in `MbrainHardwareManager.kt`.
- Ensure the library is correctly packaged in the APK and loaded via `System.loadLibrary()`.

## 🔗 References
- **Requirement**: R405c (Samsung Stay-Alive Hardening)
- **Cycle**: July.24.06

# Issue #543: Missing Native Library Dependency (`libmbrainSDK`)

## 🎯 Status: Resolved (July.25.03)
**Category**: Hardware Integration / Compatibility

---

## 📝 Description
The system was failing to load the vendor-specific `libmbrainSDK` native library on target hardware. Logcat showed `initMbrain failed`.

## 🔍 Resolution
- Established JNI infrastructure with `MbrainHardwareManager.kt` and `mbrain-jni.cpp`.
- Integrated `externalNativeBuild` using CMake in `app/build.gradle`.
- Updated `TrackerService` to initialize the library on startup and utilize `punchHardware()` for more efficient chipset keep-alive signaling on Samsung A15/MediaTek devices.

## 🔗 References
- **Requirement**: R405c (Samsung Stay-Alive Hardening)
- **Cycle**: July.25.03

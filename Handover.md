# Forensic Handover - v8.9.86 (FGS Hardened)

## 📌 Status: Stable / Build PASS
Issue #025 (FGS Transition Timeout) has been resolved. The system now utilizes a 45s user-interaction pulse window to ensure stable Foreground Service transitions on Android 14+.

### 🟢 Completed: Issue #025 (FGS Transition Timeout)
*   **Timeout Relaxation**: Increased `UI_PULSE_TIMEOUT_MS` from 15s to 45s in `EngineConstants.kt`.
*   **Android 14+ Hardening**: This provides a robust window for the system to claim `MICROPHONE` and `LOCATION` FGS types during automated state transitions (e.g., Physical Tamper detection) when the device is unattended.
*   **Consistency**: Aligned the pulse window with `FGS_STICKY_DELAY_MS` to ensure capability transitions remain valid during service updates.

### 🟢 Previously Completed
*   **Issue #024**: Accuracy Window Aliasing resolved via bucket expansion.
*   **Issue #023**: DataStore Binary Incompatibility resolved.
*   **Issue #021**: Fixed Map UI infinite loop.
*   **Issue #020**: Map centering race condition resolved.
*   **Issue #014**: Full stack standardized to `Double`.

### 🟡 Pending Validation
*   **Soak Test Monitoring**: Ongoing 24-hour stability test for `STABILITY GAP` logs.
*   **Transition Verification**: Perform unattended physical tamper tests to verify FGS type escalation without `ForegroundServiceStartNotAllowedException`.

### 🛠 Instructions for Resumption
1.  **Build System**: Run `./gradlew :app:assembleDebug`.
2.  **Verify FGS State**: In Tracker mode, trigger an "Acoustic Alert" while the app is backgrounded and verify the notification updates to "Acoustic monitoring active" without service crashes.

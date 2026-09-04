# Resolution Archive (Sep.04.20)

## 🟢 Resolved Issues (Sep.04.20)
*   **Issue #905 RESOLVED: Global GNSS Reception Hardening**. Expanded revival pulse logic in `HardwareProvider` to include `SIGNAL_LOSS` and `GPS_GAP` states. Remediates Samsung A15/S21FE "Zombie GNSS" failure where 0 satellites are reported indefinitely by forcing a hardware-level location update restart (R905/R-ID 252).

## 🟢 Recently Resolved Issues (Sep.04.10)
*   **Issue #906 RESOLVED: Signaling Transport Robustness**. Remediated critical "SRV Red" failures by removing strict `websocket` transport enforcement in `CommunicationManager`. Allowed default `socket.io` polling-to-websocket upgrade handshake, ensuring connectivity stability across diverse network environments and budget hardware like the Samsung A15 (R906/R251).

## 🟢 Recently Resolved Issues (Sep.04.05)
*   **Issue #901 RESOLVED: Log Spam Regression**. Hardened `GpsApplication.trimCaches()` to preserve "pkg" and "uid" identity tokens in `ShadowCache`. Previous implementation cleared these tokens, triggering fallback to native `getPackageName()` and resulting in persistent IPC diagnostic log spam on Samsung hardware (R759).

## 🟢 Recently Resolved Issues (Sep.04.01)
*   **Issue #900 RESOLVED: Background Service Restriction (A15)**. Hardened `PhoneSetupOverlay` with explicit guidance for Samsung "Unrestricted" battery mode. Added `isSamsungDevice` detection to `SystemStatusProvider` to trigger targeted UI instructions, remediating `BackgroundServiceStartNotAllowedException` on budget hardware (R900).
*   **Issue #904 RESOLVED: GNSS Rejection (Confirmed)**. Implemented targeted guidance for "Precise Location" in `PhoneSetupOverlay` (Step 0) for Samsung devices. Verified that OS-level coarse location downgrades were the root cause of `0/0` satellite visibility on A15 hardware (R904).

## 🟢 Recently Resolved Issues (Sep.04.01)
*   **Issue #898: A15 Connectivity & GPS Hardening**. Budget hardware (A15) showed intermittent signaling loss and GPS staleness. Implemented multi-tier hardening: Reduced radio poke interval to 30s and forced 10s GPS polling when screen is off (R898).

## 🟢 Recently Resolved Issues (Sep.03.190)
*   **Issue #899 RESOLVED: Multi-Device Field Test (S21FE -> A15)**. Executed deployment and monitoring of version Sep.03.1xx on both devices. Verified Tracker UI on A15; confirmed critical GNSS rejection (#904) and background service start restrictions (#900) on budget hardware (Target SDK 35). Readiness prep complete (R899/R250).

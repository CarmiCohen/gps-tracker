# SOT Master Requirements (Sep.01.17)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (36 Rules)

### 1. Lifecycle & Resource Management
...
*   **1.12 Hardware Disposal (R887/R888/R889/R890)**: **MANDATORY**. All hardware callbacks (Sensors, GNSS, Network, Display, Location) MUST be unregistered using the `ManagedHardware` synchronization pattern, utilizing the `ManagedUnregistrationHelper` for consistent hardening (R889). To prevent native `BaseEventQueue` leaks, the unregistration MUST utilize a 4000ms timeout and implement a final direct fallback unregistration on the current thread if the synchronization latch expires. Furthermore, `HardwareProvider.stop()` MUST implement a 500ms settling window after unregistration to ensure native disposal completes before `HandlerThread` termination (R890). (Updated Sep.01.17).
...

*(Total: 36 Architectural Rules + 197 Functional R-IDs = 233 Items)*

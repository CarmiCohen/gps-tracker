# Simplification Ideas (Sep.06.00)

## 🎯 Architecture & Logic
- **[A021] Managed Lifecycle Delegate**: Create a reusable delegate to handle the `Job? = null` + `cancel()` + `launch` pattern used for async tasks in services. This would remove repetitive boilerplate from `HardwareProvider` and `TrackerService`.
- **[A022] Forensic Auditor Extraction**: Extract GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) from `HardwareProvider` into a standalone `ForensicAuditor`. This would restore `HardwareProvider` to a clean bridge for hardware events.
- **[A023] Revival Logic Unification**: Unify the Fused Location burst and Raw Provider bypass logic into a single `LocationReviver` component. The current implementation in `HardwareProvider` is becoming complex.

## ⚙️ Performance & Efficiency
- **[P015] Circular State Buffers**: Replace the fixed-array snr and sensor buffers with a generic `CircularBuffer<T>` to simplify indexing logic and prevent manual synchronization errors.

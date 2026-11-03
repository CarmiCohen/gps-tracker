# Issues Log

## Active Issues & Warnings

### 1. Compose SnapshotStateList Lock Verification Warning
- **Component**: `androidx.compose.runtime.snapshots.SnapshotStateList`
- **Severity**: Warning
- **Description**: 
  ```
  Method boolean androidx.compose.runtime.snapshots.SnapshotStateList.conditionalUpdate(boolean, kotlin.jvm.functions.Function1) failed lock verification and will run slower.
  ```
- **Impact**: Non-blocking performance warning during Compose state updates on Android runtime.
- **Status**: Monitored. No crashes or functional failures observed.

### 2. Flexible Tracker / Device ID Validation
- **Component**: `com.jd.core.shared.ForensicIdentity` & `MonitorActivity`
- **Severity**: Enhancement / Requirement Change
- **Description**: Relax strict 64-character SHA-256 hex hash validation on the "Enter Tracker ID" field to allow any sequence of characters (including a single character).
- **Status**: Implemented. `ForensicIdentity.isValid` updated to accept any non-blank character sequence.

# Resolution Archive - GPS Tracker

## ISSUE-OCT19.19-F1: Test Mode Restoration Bypass (Oct19.19)
- **Resolved**: Oct19.19
- **Description**: Automatic "Last Mode Restoration" (R-ID 242) prevented manual mode switching in test scenarios where the app is force-stopped and relaunched, as it immediately re-entered the previous mode (usually Tracker).
- **Remediation**:
    - **Persistence Hook (R-ID 743)**: Added `is_restoration_disabled` to `AppSettings` DataStore.
    - **ViewModel Orchestration**: Updated `MainViewModel` to check this flag before emitting `UiEffect.StartService` on cold start.
    - **UI Diagnostic**: Integrated a "Skip Mode Restoration" toggle in the Diagnostics screen under "Validation Hooks".
- **Outcome**: 🟢 **PASSED**. Testing of cross-mode transitions is now deterministic. R-ID 242 can be bypassed persistently. (SOT ID 773).

## ISSUE-OCT18.22-F2: Redundant Network Callback Registrations (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: Multiple components (SystemStatusProviderImpl and AndroidNetworkProvider) were independently registering ConnectivityManager.NetworkCallback instances. This caused redundant system overhead and frequent "Registering system network callback" stack logs in forensic traces.
- **Remediation**:
    - **Unified Authority (R-ID 742)**: Consolidated all ConnectivityManager callback management into `AndroidNetworkProvider`.
    - **Reactive Integration**: Implemented `isNetworkAvailableFlow` using `callbackFlow` with `shareIn` to provide a single system-wide stream of internet availability.
    - **Provider Refactoring**: Updated `SystemStatusProviderImpl` to consume the unified flow, eliminating its internal registration logic.
- **Outcome**: 🟢 **PASSED**. Redundant registrations are eliminated. The system now maintains exactly one network callback registration regardless of the number of active observers. (SOT ID 772).

... [Remaining historical entries preserved] ...

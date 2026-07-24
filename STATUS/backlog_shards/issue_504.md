# Issue #504: Position EMA Implementation

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Filtering

---

## 📝 Description
The legacy Interacting Multiple Model (IMM) filter was decommissioned due to excessive computational complexity and occasional divergence on budget hardware. It required replacement with a high-performance Position EMA (Exponential Moving Average) filter.

## 🛠️ Resolution
- Decommissioned `ImmFilter.kt`.
- Implemented a two-tier Position EMA in `LocationProcessor.kt`.
- Established distinct alpha factors for Stationary (`0.1`) and Moving (`0.3`) states to balance responsiveness with urban canyon jitter suppression.

## 🔗 References
- **Requirement**: R999 (Type Safety Authority)
- **Files**: `ImmFilter.kt`, `EngineConstants.kt`

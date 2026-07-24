# Issue #865: Unified Identity Green Enforcement

## 🎯 Status: Resolved (Historical)
**Category**: UI / Branding

---

## 📝 Description
Enforcement of the authoritative JD Green (#367C2B) as the primary identity color for the Tracker role, replacing inconsistent green variants used in early UI components.

## 🛠️ Resolution
- Defined `BrandJd` in `Color.kt` as the authoritative primary color.
- Synchronized `colors.xml` and Compose themes to use the unified variant.
- Verified forensic parity in the "SRV" badge and status indicators.

## 🔗 References
- **Requirement**: R865 (Identity Authority)
- **File**: `app/src/main/java/com/gps19/app/Color.kt`

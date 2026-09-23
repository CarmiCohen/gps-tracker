package com.gps19.core.engine

/**
 * HardwareSot: Core engine authority for hardware identification and capability gating.
 * Sep.23.08:
 * - Issue #1204: Unified Hardware Lifecycle. Added Huawei detection and 
 *   consolidated vendor identification authority.
 * Sep.16.05:
 * - Issue #1060: Capability Consolidation. Transitioned authority to 
 *   PerformanceTier enum (R-ID 348).
 * Sep.16.00:
 * - Issue #1055: Unified Performance Tier. Added isStaggeredPerformanceTier
 *   to harmonize A15 and S21FE remediation (R-ID 348, formerly R-ID 347).
 */
object HardwareSot {

    /**
     * Identifies Samsung devices based on Manufacturer or Brand.
     */
    fun isSamsung(manufacturer: String, brand: String): Boolean {
        val m = manufacturer.uppercase()
        val b = brand.uppercase()
        return m.contains("SAMSUNG") || b.contains("SAMSUNG")
    }

    /**
     * Identifies Xiaomi/Redmi/Poco devices.
     */
    fun isXiaomi(manufacturer: String): Boolean {
        val m = manufacturer.uppercase()
        return m.contains("XIAOMI") || m.contains("REDMI") || m.contains("POCO")
    }

    /**
     * Identifies Huawei/Honor devices.
     */
    fun isHuawei(manufacturer: String, brand: String): Boolean {
        val m = manufacturer.uppercase()
        val b = brand.uppercase()
        return m.contains("HUAWEI") || m.contains("HONOR") || b.contains("HUAWEI") || b.contains("HONOR")
    }

    /**
     * Hardened S21FE detection covering G990B/E/U and generic variants.
     */
    fun isS21FE(manufacturer: String, brand: String, model: String): Boolean {
        if (!isSamsung(manufacturer, brand)) return false
        val m = model.uppercase()
        return m.contains("G990") || m.contains("S21FE")
    }

    /**
     * Hardened A15 detection (R405).
     * Uses Model, Product, and Device strings to catch all variants of SM-A155/SM-A156.
     */
    fun isA15(manufacturer: String, brand: String, model: String, product: String, device: String): Boolean {
        if (!isSamsung(manufacturer, brand)) return false
        val m = model.uppercase()
        val p = product.uppercase()
        val d = device.uppercase()
        return m.contains("A15") || p.contains("A15") || d.contains("A15")
    }

    /**
     * Identifies devices in the "staggered performance" tier (A15, S21FE) 
     * that require relaxed latency thresholds and throttled telemetry (R-ID 348).
     */
    fun isStaggeredPerformanceTier(manufacturer: String, brand: String, model: String, product: String, device: String): Boolean {
        return isA15(manufacturer, brand, model, product, device) || isS21FE(manufacturer, brand, model)
    }
}

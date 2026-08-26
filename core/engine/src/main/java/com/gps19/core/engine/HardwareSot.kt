package com.gps19.core.engine

/**
 * HardwareSot: Core engine authority for hardware identification and capability gating.
 * Aug.25.05:
 * - Issue #317: Architectural Decoupling. Migrated detection logic from :app layer 
 *   to core:engine to allow standalone hardware awareness (R313/R212).
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
}

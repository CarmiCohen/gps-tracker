package com.jd.core.shared

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoUtils: the hash behind the forensic identities.
 * Updated Revol29.4: Standardized hex output for forensic identities.
 */
object CryptoUtils {

    /**
     * Hashes ANDROID_ID with a persistent salt to create a unique Tracker ID (Point 25).
     * Returns a 64-character hex string (SHA-256).
     */
    fun hashIdentity(androidId: String, salt: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt.toByteArray(), "HmacSHA256"))
        val hashBytes = mac.doFinal(androidId.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

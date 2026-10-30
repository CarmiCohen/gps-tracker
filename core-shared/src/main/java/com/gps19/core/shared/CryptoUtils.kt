package com.gps19.core.shared

import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * CryptoUtils: Hardened security for Project Revolution.
 * Implements HMAC-SHA256 for handshakes and AES-GCM for E2EE payloads (Point 5/142).
 * Updated Revol29.4: Standardized hex output for forensic identities.
 */
object CryptoUtils {
    
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun generateNonce(): String = UUID.randomUUID().toString()

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

    /**
     * encryptPayload: AES-GCM E2EE for telemetry and commands (Point 142).
     */
    fun encryptPayload(data: ByteArray, secretKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"), spec)
        
        val ciphertext = cipher.doFinal(data)
        return iv + ciphertext // Prepend IV for decryption
    }

    fun decryptPayload(encryptedData: ByteArray, secretKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = encryptedData.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = encryptedData.sliceArray(GCM_IV_LENGTH until encryptedData.size)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"), spec)
        return cipher.doFinal(ciphertext)
    }

    fun signHandshake(nonce: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(nonce.toByteArray()))
    }
}

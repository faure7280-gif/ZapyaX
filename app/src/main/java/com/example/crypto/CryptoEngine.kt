package com.example.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    fun generate256BitKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(256, secureRandom)
        return keyGen.generateKey()
    }

    fun keyToBase64(secretKey: SecretKey): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(secretKey.encoded)
        } catch (e: Throwable) {
            Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
        }
    }

    fun keyFromBase64(base64: String): SecretKey {
        val decoded = try {
            java.util.Base64.getDecoder().decode(base64)
        } catch (e: Throwable) {
            Base64.decode(base64, Base64.NO_WRAP)
        }
        return SecretKeySpec(decoded, ALGORITHM)
    }

    fun generateFingerprint(secretKey: SecretKey): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(secretKey.encoded)
        val hex = digest.joinToString("") { "%02X".format(it) }
        return hex.take(16).chunked(4).joinToString(" : ")
    }

    fun encrypt(data: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherText = cipher.doFinal(data)

        // Result = IV (12 bytes) + CipherText (with 16-byte GCM auth tag appended)
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return combined
    }

    fun decrypt(encryptedPayload: ByteArray, secretKey: SecretKey): ByteArray {
        require(encryptedPayload.size >= GCM_IV_LENGTH) { "Payload demasiado corto para contener IV" }

        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedPayload, 0, iv, 0, GCM_IV_LENGTH)

        val cipherTextSize = encryptedPayload.size - GCM_IV_LENGTH
        val cipherText = ByteArray(cipherTextSize)
        System.arraycopy(encryptedPayload, GCM_IV_LENGTH, cipherText, 0, cipherTextSize)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherText)
    }

    fun encryptChunk(chunk: ByteArray, secretKey: SecretKey, chunkIndex: Int): ByteArray {
        return encrypt(chunk, secretKey)
    }

    fun decryptChunk(encryptedChunk: ByteArray, secretKey: SecretKey, chunkIndex: Int): ByteArray {
        return decrypt(encryptedChunk, secretKey)
    }
}

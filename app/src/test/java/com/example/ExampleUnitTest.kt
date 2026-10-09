package com.example

import com.example.crypto.CryptoEngine
import com.example.model.PeerConnectionInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun testAes256GcmEncryptionDecryption() {
        val key = CryptoEngine.generate256BitKey()
        val originalData = "ZapyaX High-Speed Parallel E2EE Test Data".toByteArray(Charsets.UTF_8)

        val encrypted = CryptoEngine.encrypt(originalData, key)
        assertTrue(encrypted.size > originalData.size)

        val decrypted = CryptoEngine.decrypt(encrypted, key)
        assertArrayEquals(originalData, decrypted)
        assertEquals("ZapyaX High-Speed Parallel E2EE Test Data", String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun testQrPayloadSerializationAndDeserialization() {
        val key = CryptoEngine.generate256BitKey()
        val keyB64 = CryptoEngine.keyToBase64(key)
        val fp = CryptoEngine.generateFingerprint(key)

        val originalPeer = PeerConnectionInfo(
            ipAddress = "192.168.1.45",
            port = 8989,
            sessionId = "SESSION123",
            deviceName = "Pixel 9 Pro",
            secretKeyBase64 = keyB64,
            fingerprint = fp
        )

        val payload = originalPeer.toQrPayload()
        val parsedPeer = PeerConnectionInfo.fromQrPayload(payload)

        assertNotNull(parsedPeer)
        assertEquals(originalPeer.ipAddress, parsedPeer?.ipAddress)
        assertEquals(originalPeer.port, parsedPeer?.port)
        assertEquals(originalPeer.sessionId, parsedPeer?.sessionId)
        assertEquals(originalPeer.deviceName, parsedPeer?.deviceName)
        assertEquals(originalPeer.secretKeyBase64, parsedPeer?.secretKeyBase64)
    }

    @Test
    fun testChunkEncryptionDecryption() {
        val key = CryptoEngine.generate256BitKey()
        val chunkData = ByteArray(64 * 1024) { (it % 128).toByte() }

        val encryptedChunk = CryptoEngine.encryptChunk(chunkData, key, chunkIndex = 0)
        val decryptedChunk = CryptoEngine.decryptChunk(encryptedChunk, key, chunkIndex = 0)

        assertArrayEquals(chunkData, decryptedChunk)
    }
}

package com.example.network

import android.content.Context
import com.example.crypto.CryptoEngine
import com.example.model.FileCategory
import com.example.model.TransferFile
import com.example.model.TransferStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.crypto.SecretKey

class TransferServer(
    private val context: Context,
    private val port: Int = 8989,
    private val expectedSessionId: String,
    private val secretKey: SecretKey,
    private val onFileProgress: (fileId: String, channelId: Int, progress: Float, speedMBps: Float, transferredBytes: Long) -> Unit,
    private val onFileCompleted: (TransferFile) -> Unit,
    private val onError: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                while (isActive && isRunning) {
                    val clientSocket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: Exception) {
                        break
                    }
                    // Handle each channel/connection concurrently
                    launch {
                        handleClientConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    onError("Error en servidor receptor: ${e.message}")
                }
            }
        }
    }

    private fun handleClientConnection(socket: Socket) {
        socket.use { s ->
            s.tcpNoDelay = true
            s.sendBufferSize = 128 * 1024
            s.receiveBufferSize = 128 * 1024

            val din = DataInputStream(s.getInputStream().buffered(64 * 1024))
            val dout = DataOutputStream(s.getOutputStream().buffered(16 * 1024))

            try {
                val handshake = TransferProtocol.readHandshake(din)
                if (handshake.sessionId != expectedSessionId) {
                    dout.writeUTF("ERR_INVALID_SESSION")
                    dout.flush()
                    return
                }
                dout.writeUTF("OK_AUTH_E2EE")
                dout.flush()

                val channelId = handshake.channelId

                while (isRunning) {
                    val msgType = try {
                        din.readByte()
                    } catch (e: Exception) {
                        break
                    }

                    when (msgType) {
                        TransferProtocol.MSG_FILE_HEADER -> {
                            val fileId = din.readUTF()
                            val fileName = din.readUTF()
                            val fileSize = din.readLong()
                            val mimeType = din.readUTF()

                            val storageDir = com.example.data.DeviceStorageHelper.getReceivedDirectory(context)
                            val destFile = File(storageDir, fileName)

                            var totalReceived = 0L
                            var lastTime = System.currentTimeMillis()
                            var bytesSinceLastSpeed = 0L
                            var currentSpeedMBps = 0f

                            FileOutputStream(destFile).use { fos ->
                                while (totalReceived < fileSize) {
                                    val chunkTag = din.readByte()
                                    if (chunkTag != TransferProtocol.MSG_CHUNK_DATA) break

                                    val chunkIndex = din.readInt()
                                    val payloadLength = din.readInt()
                                    val encryptedPayload = ByteArray(payloadLength)
                                    din.readFully(encryptedPayload)

                                    // Decrypt chunk with authenticated AES-256-GCM
                                    val plainChunk = CryptoEngine.decryptChunk(encryptedPayload, secretKey, chunkIndex)
                                    fos.write(plainChunk)

                                    totalReceived += plainChunk.size
                                    bytesSinceLastSpeed += plainChunk.size

                                    val now = System.currentTimeMillis()
                                    val elapsed = now - lastTime
                                    if (elapsed >= 400 || totalReceived == fileSize) {
                                        currentSpeedMBps = (bytesSinceLastSpeed / (1024f * 1024f)) / (elapsed / 1000f)
                                        lastTime = now
                                        bytesSinceLastSpeed = 0L
                                    }

                                    val progress = (totalReceived.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f)
                                    onFileProgress(fileId, channelId, progress, currentSpeedMBps, totalReceived)
                                }
                            }

                            val endMsg = din.readByte()
                            if (endMsg == TransferProtocol.MSG_FILE_END) {
                                dout.writeByte(TransferProtocol.MSG_FILE_END.toInt())
                                dout.flush()

                                val receivedItem = TransferFile(
                                    id = fileId,
                                    name = fileName,
                                    sizeBytes = fileSize,
                                    mimeType = mimeType,
                                    category = deduceCategory(mimeType, fileName),
                                    progress = 1f,
                                    status = TransferStatus.COMPLETED,
                                    channelId = channelId,
                                    speedMBps = currentSpeedMBps,
                                    transferredBytes = fileSize,
                                    isEncrypted = true,
                                    localFilePath = destFile.absolutePath
                                )
                                onFileCompleted(receivedItem)
                                com.example.data.DeviceStorageHelper.notifyMediaScanner(context, destFile, mimeType)
                            }
                        }

                        TransferProtocol.MSG_SESSION_CLOSE -> {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Connection closed or finished
            }
        }
    }

    private fun deduceCategory(mime: String, name: String): FileCategory {
        val lower = name.lowercase()
        return when {
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".webp") -> FileCategory.PHOTOS
            mime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") -> FileCategory.VIDEOS
            mime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") -> FileCategory.AUDIO
            mime.startsWith("text/") || lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt") -> FileCategory.DOCUMENTS
            lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".apk") || lower.endsWith(".tar") -> FileCategory.ARCHIVES
            else -> FileCategory.DOCUMENTS
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverJob?.cancel()
    }
}

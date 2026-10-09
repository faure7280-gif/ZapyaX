package com.example.network

import android.content.Context
import android.net.Uri
import com.example.crypto.CryptoEngine
import com.example.model.TransferFile
import com.example.model.TransferStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.crypto.SecretKey

class TransferClient(
    private val context: Context,
    private val targetIp: String,
    private val targetPort: Int,
    private val sessionId: String,
    private val secretKey: SecretKey,
    private val deviceName: String,
    private val onFileProgress: (fileId: String, channelId: Int, progress: Float, speedMBps: Float, transferredBytes: Long) -> Unit,
    private val onFileCompleted: (TransferFile) -> Unit,
    private val onAllCompleted: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var clientJob: Job? = null
    private var isCancelled = false

    fun startTransfer(filesToTransfer: List<TransferFile>) {
        if (filesToTransfer.isEmpty()) {
            onAllCompleted()
            return
        }

        isCancelled = false
        val queue = ArrayDeque(filesToTransfer)
        val queueMutex = Mutex()

        clientJob = scope.launch {
            try {
                // Dual Stream parallel workers: Channel 1 and Channel 2
                val worker1 = launch {
                    runChannelWorker(channelId = 1, queue, queueMutex)
                }
                val worker2 = launch {
                    runChannelWorker(channelId = 2, queue, queueMutex)
                }

                worker1.join()
                worker2.join()

                if (!isCancelled) {
                    onAllCompleted()
                }
            } catch (e: Exception) {
                if (!isCancelled) {
                    onError("Error en transferencia dual: ${e.message}")
                }
            }
        }
    }

    private suspend fun runChannelWorker(
        channelId: Int,
        queue: ArrayDeque<TransferFile>,
        queueMutex: Mutex
    ) {
        var socket: Socket? = null
        var din: DataInputStream? = null
        var dout: DataOutputStream? = null

        try {
            while (!isCancelled && scope.isActive) {
                val nextFile = queueMutex.withLock {
                    if (queue.isNotEmpty()) queue.removeFirst() else null
                } ?: break // Queue empty, worker finished

                // Ensure socket is connected for this channel worker
                if (socket == null || socket.isClosed || !socket.isConnected) {
                    socket = Socket().apply {
                        tcpNoDelay = true
                        sendBufferSize = 128 * 1024
                        receiveBufferSize = 64 * 1024
                        connect(InetSocketAddress(targetIp, targetPort), 6000)
                    }
                    dout = DataOutputStream(socket.getOutputStream().buffered(64 * 1024))
                    din = DataInputStream(socket.getInputStream().buffered(16 * 1024))

                    TransferProtocol.writeHandshake(dout, sessionId, channelId, deviceName)
                    val authAck = din.readUTF()
                    if (authAck != "OK_AUTH_E2EE") {
                        throw IllegalStateException("Fallo de autenticación E2EE en canal $channelId: $authAck")
                    }
                }

                sendFileOverStream(
                    fileItem = nextFile,
                    channelId = channelId,
                    din = din!!,
                    dout = dout!!
                )
            }

            dout?.writeByte(TransferProtocol.MSG_SESSION_CLOSE.toInt())
            dout?.flush()
        } catch (e: Exception) {
            if (!isCancelled) {
                onError("Canal $channelId error: ${e.message}")
            }
        } finally {
            try { socket?.close() } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun sendFileOverStream(
        fileItem: TransferFile,
        channelId: Int,
        din: DataInputStream,
        dout: DataOutputStream
    ) {
        val inputStream: InputStream = openInputStream(fileItem)

        dout.writeByte(TransferProtocol.MSG_FILE_HEADER.toInt())
        dout.writeUTF(fileItem.id)
        dout.writeUTF(fileItem.name)
        dout.writeLong(fileItem.sizeBytes)
        dout.writeUTF(fileItem.mimeType)
        dout.flush()

        val buffer = ByteArray(TransferProtocol.CHUNK_SIZE)
        var chunkIndex = 0
        var totalSent = 0L
        var lastTime = System.currentTimeMillis()
        var bytesSinceLastSpeed = 0L
        var currentSpeedMBps = 0f

        inputStream.use { stream ->
            while (!isCancelled) {
                val bytesRead = stream.read(buffer)
                if (bytesRead == -1) break

                val plainChunk = if (bytesRead == buffer.size) {
                    buffer
                } else {
                    buffer.copyOf(bytesRead)
                }

                // AES-256-GCM on-the-fly encryption per chunk
                val encryptedPayload = CryptoEngine.encryptChunk(plainChunk, secretKey, chunkIndex)

                dout.writeByte(TransferProtocol.MSG_CHUNK_DATA.toInt())
                dout.writeInt(chunkIndex)
                dout.writeInt(encryptedPayload.size)
                dout.write(encryptedPayload)
                dout.flush()

                totalSent += bytesRead
                bytesSinceLastSpeed += bytesRead
                chunkIndex++

                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 300 || totalSent == fileItem.sizeBytes) {
                    currentSpeedMBps = (bytesSinceLastSpeed / (1024f * 1024f)) / (elapsed / 1000f)
                    lastTime = now
                    bytesSinceLastSpeed = 0L
                }

                val progress = if (fileItem.sizeBytes > 0) {
                    (totalSent.toFloat() / fileItem.sizeBytes.toFloat()).coerceIn(0f, 1f)
                } else 1f

                onFileProgress(fileItem.id, channelId, progress, currentSpeedMBps, totalSent)
            }
        }

        if (!isCancelled) {
            dout.writeByte(TransferProtocol.MSG_FILE_END.toInt())
            dout.flush()

            val ack = din.readByte()
            if (ack == TransferProtocol.MSG_FILE_END) {
                val completedFile = fileItem.copy(
                    progress = 1f,
                    status = TransferStatus.COMPLETED,
                    channelId = channelId,
                    speedMBps = currentSpeedMBps,
                    transferredBytes = fileItem.sizeBytes
                )
                onFileCompleted(completedFile)
            }
        }
    }

    private fun openInputStream(fileItem: TransferFile): InputStream {
        return when {
            fileItem.isSimulated -> {
                // Generate high throughput test bytes for rapid demonstration
                val sampleData = ByteArray(fileItem.sizeBytes.coerceAtMost(1024 * 1024).toInt()) { (it % 127).toByte() }
                ByteArrayInputStream(sampleData)
            }
            fileItem.uriString != null -> {
                context.contentResolver.openInputStream(Uri.parse(fileItem.uriString))
                    ?: throw IllegalStateException("No se pudo abrir URI: ${fileItem.uriString}")
            }
            fileItem.localFilePath != null -> {
                FileInputStream(File(fileItem.localFilePath))
            }
            else -> {
                ByteArrayInputStream(ByteArray(0))
            }
        }
    }

    fun cancel() {
        isCancelled = true
        scope.launch {
            clientJob?.cancelAndJoin()
        }
    }
}

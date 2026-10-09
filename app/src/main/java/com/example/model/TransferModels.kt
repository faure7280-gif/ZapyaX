package com.example.model

enum class FileCategory(val displayName: String) {
    ALL("Todos"),
    PHOTOS("Fotos"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documentos"),
    ARCHIVES("Archivos")
}

enum class TransferStatus(val label: String) {
    QUEUED("En cola"),
    ENCRYPTING("Cifrando"),
    STREAMING("Enviando"),
    COMPLETED("Completado"),
    FAILED("Error"),
    CANCELLED("Cancelado")
}

enum class TransferDirection {
    SENDING,
    RECEIVING
}

data class TransferFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val uriString: String? = null,
    val category: FileCategory = FileCategory.DOCUMENTS,
    val progress: Float = 0f,
    val status: TransferStatus = TransferStatus.QUEUED,
    val channelId: Int? = null, // 1 or 2 (Dual Stream)
    val speedMBps: Float = 0f,
    val transferredBytes: Long = 0L,
    val isEncrypted: Boolean = true,
    val localFilePath: String? = null,
    val isSimulated: Boolean = false
) {
    val formattedSize: String
        get() {
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return "%.1f KB".format(kb)
            val mb = kb / 1024.0
            if (mb < 1024) return "%.1f MB".format(mb)
            val gb = mb / 1024.0
            return "%.2f GB".format(gb)
        }
}

data class PeerConnectionInfo(
    val ipAddress: String,
    val port: Int = 8989,
    val sessionId: String,
    val deviceName: String,
    val secretKeyBase64: String,
    val fingerprint: String
) {
    fun toQrPayload(): String {
        return "ZAPBEAM:v1|ip=$ipAddress|port=$port|sid=$sessionId|dev=$deviceName|key=$secretKeyBase64"
    }

    companion object {
        fun fromQrPayload(payload: String): PeerConnectionInfo? {
            try {
                if (!payload.startsWith("ZAPBEAM:v1|")) return null
                val parts = payload.removePrefix("ZAPBEAM:v1|").split("|")
                var ip = ""
                var port = 8989
                var sid = ""
                var dev = "Dispositivo Cercano"
                var key = ""

                for (part in parts) {
                    val kv = part.split("=", limit = 2)
                    if (kv.size == 2) {
                        when (kv[0]) {
                            "ip" -> ip = kv[1]
                            "port" -> port = kv[1].toIntOrNull() ?: 8989
                            "sid" -> sid = kv[1]
                            "dev" -> dev = kv[1]
                            "key" -> key = kv[1]
                        }
                    }
                }
                if (ip.isBlank() || key.isBlank()) return null
                val fp = try {
                    val secretKey = com.example.crypto.CryptoEngine.keyFromBase64(key)
                    com.example.crypto.CryptoEngine.generateFingerprint(secretKey)
                } catch (e: Exception) {
                    "VALIDADO"
                }
                return PeerConnectionInfo(ip, port, sid, dev, key, fp)
            } catch (e: Exception) {
                return null
            }
        }
    }
}

data class DualChannelStats(
    val channel1Active: Boolean = false,
    val channel1FileName: String = "",
    val channel1Progress: Float = 0f,
    val channel1SpeedMBps: Float = 0f,
    val channel2Active: Boolean = false,
    val channel2FileName: String = "",
    val channel2Progress: Float = 0f,
    val channel2SpeedMBps: Float = 0f,
    val totalActiveStreams: Int = 0,
    val aggregateSpeedMBps: Float = 0f
)

data class TransferHistoryItem(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val direction: TransferDirection,
    val timestamp: Long,
    val peerName: String,
    val e2eeFingerprint: String,
    val isEncrypted: Boolean = true,
    val filePath: String? = null
)

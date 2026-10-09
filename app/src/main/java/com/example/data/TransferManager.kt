package com.example.data

import android.content.Context
import com.example.crypto.CryptoEngine
import com.example.model.DualChannelStats
import com.example.model.FileCategory
import com.example.model.PeerConnectionInfo
import com.example.model.TransferDirection
import com.example.model.TransferFile
import com.example.model.TransferHistoryItem
import com.example.model.TransferStatus
import com.example.network.NetworkUtils
import com.example.network.PeerDiscoveryManager
import com.example.network.TransferClient
import com.example.network.TransferServer
import com.example.service.TransferForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.crypto.SecretKey

class TransferManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    // Current receiver session credentials
    private var currentSecretKey: SecretKey = CryptoEngine.generate256BitKey()
    private var currentSessionId: String = UUID.randomUUID().toString().take(8)

    private val _myConnectionInfo = MutableStateFlow<PeerConnectionInfo?>(null)
    val myConnectionInfo: StateFlow<PeerConnectionInfo?> = _myConnectionInfo.asStateFlow()

    private val _activePeer = MutableStateFlow<PeerConnectionInfo?>(null)
    val activePeer: StateFlow<PeerConnectionInfo?> = _activePeer.asStateFlow()

    private val _isServerListening = MutableStateFlow(false)
    val isServerListening: StateFlow<Boolean> = _isServerListening.asStateFlow()

    // Selected files for sending
    private val _selectedFiles = MutableStateFlow<List<TransferFile>>(emptyList())
    val selectedFiles: StateFlow<List<TransferFile>> = _selectedFiles.asStateFlow()

    // Device real media files loaded from MediaStore
    private val _deviceMediaFiles = MutableStateFlow<List<TransferFile>>(emptyList())
    val deviceMediaFiles: StateFlow<List<TransferFile>> = _deviceMediaFiles.asStateFlow()

    // Active transfers
    private val _activeTransfers = MutableStateFlow<List<TransferFile>>(emptyList())
    val activeTransfers: StateFlow<List<TransferFile>> = _activeTransfers.asStateFlow()

    // Dual Channel statistics
    private val _dualChannelStats = MutableStateFlow(DualChannelStats())
    val dualChannelStats: StateFlow<DualChannelStats> = _dualChannelStats.asStateFlow()

    private val _isTransferActive = MutableStateFlow(false)
    val isTransferActive: StateFlow<Boolean> = _isTransferActive.asStateFlow()

    private val _transferDirection = MutableStateFlow(TransferDirection.SENDING)
    val transferDirection: StateFlow<TransferDirection> = _transferDirection.asStateFlow()

    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    private val _overallSpeedMBps = MutableStateFlow(0f)
    val overallSpeedMBps: StateFlow<Float> = _overallSpeedMBps.asStateFlow()

    private val _e2eeVerified = MutableStateFlow(true)
    val e2eeVerified: StateFlow<Boolean> = _e2eeVerified.asStateFlow()

    private val _e2eeFingerprint = MutableStateFlow("")
    val e2eeFingerprint: StateFlow<String> = _e2eeFingerprint.asStateFlow()

    // History
    private val _historyItems = MutableStateFlow<List<TransferHistoryItem>>(emptyList())
    val historyItems: StateFlow<List<TransferHistoryItem>> = _historyItems.asStateFlow()

    // Discovery
    val peerDiscoveryManager = PeerDiscoveryManager(context)

    private var server: TransferServer? = null
    private var client: TransferClient? = null

    init {
        refreshMyConnectionInfo()
        loadDeviceMedia(FileCategory.ALL)
    }

    fun loadDeviceMedia(category: FileCategory) {
        scope.launch(Dispatchers.IO) {
            val media = DeviceStorageHelper.queryDeviceMedia(context, category)
            _deviceMediaFiles.value = media
        }
    }

    fun refreshMyConnectionInfo() {
        currentSecretKey = CryptoEngine.generate256BitKey()
        currentSessionId = UUID.randomUUID().toString().take(8)
        val ip = NetworkUtils.getLocalIpAddress(context)
        val devName = NetworkUtils.getDeviceDisplayName()
        val keyB64 = CryptoEngine.keyToBase64(currentSecretKey)
        val fp = CryptoEngine.generateFingerprint(currentSecretKey)

        _e2eeFingerprint.value = fp
        val info = PeerConnectionInfo(
            ipAddress = ip,
            port = 8989,
            sessionId = currentSessionId,
            deviceName = devName,
            secretKeyBase64 = keyB64,
            fingerprint = fp
        )
        _myConnectionInfo.value = info

        if (_isServerListening.value) {
            peerDiscoveryManager.startBroadcasting(info)
        }
    }

    fun startReceiverServer() {
        if (_isServerListening.value) return
        val info = _myConnectionInfo.value ?: return

        server = TransferServer(
            context = context,
            port = info.port,
            expectedSessionId = info.sessionId,
            secretKey = currentSecretKey,
            onFileProgress = { fileId, channelId, progress, speed, transferred ->
                scope.launch {
                    _isTransferActive.value = true
                    _transferDirection.value = TransferDirection.RECEIVING
                    updateActiveFileProgress(fileId, channelId, progress, speed, transferred)
                }
            },
            onFileCompleted = { file ->
                scope.launch {
                    markFileCompleted(file, TransferDirection.RECEIVING, "Emisor ZapyaX")
                }
            },
            onError = { errMsg ->
                scope.launch {
                    finishTransferSession()
                }
            }
        )
        server?.start()
        _isServerListening.value = true
        _transferDirection.value = TransferDirection.RECEIVING

        // Broadcast to local Wi-Fi / Hotspot for automatic discovery
        peerDiscoveryManager.startBroadcasting(info)
    }

    fun stopReceiverServer() {
        peerDiscoveryManager.stopBroadcasting()
        server?.stop()
        server = null
        _isServerListening.value = false
    }

    fun addSelectedFile(file: TransferFile) {
        if (_selectedFiles.value.none { it.id == file.id }) {
            _selectedFiles.value = _selectedFiles.value + file
        }
    }

    fun removeSelectedFile(fileId: String) {
        _selectedFiles.value = _selectedFiles.value.filter { it.id != fileId }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun startRealTransferToPeer(peerInfo: PeerConnectionInfo) {
        val files = _selectedFiles.value
        if (files.isEmpty()) return

        _activePeer.value = peerInfo
        _e2eeFingerprint.value = peerInfo.fingerprint
        _transferDirection.value = TransferDirection.SENDING
        _isTransferActive.value = true
        _activeTransfers.value = files.map { it.copy(status = TransferStatus.QUEUED, progress = 0f) }

        val peerSecretKey = CryptoEngine.keyFromBase64(peerInfo.secretKeyBase64)

        TransferForegroundService.start(context)

        client = TransferClient(
            context = context,
            targetIp = peerInfo.ipAddress,
            targetPort = peerInfo.port,
            sessionId = peerInfo.sessionId,
            secretKey = peerSecretKey,
            deviceName = NetworkUtils.getDeviceDisplayName(),
            onFileProgress = { fileId, channelId, progress, speed, transferred ->
                scope.launch {
                    updateActiveFileProgress(fileId, channelId, progress, speed, transferred)
                }
            },
            onFileCompleted = { file ->
                scope.launch {
                    markFileCompleted(file, TransferDirection.SENDING, peerInfo.deviceName)
                }
            },
            onAllCompleted = {
                scope.launch {
                    finishTransferSession()
                }
            },
            onError = { err ->
                scope.launch {
                    finishTransferSession()
                }
            }
        )

        client?.startTransfer(files)
    }

    private fun updateActiveFileProgress(
        fileId: String,
        channelId: Int,
        progress: Float,
        speedMBps: Float,
        transferredBytes: Long
    ) {
        val currentList = _activeTransfers.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(
                progress = progress,
                status = if (progress >= 1f) TransferStatus.COMPLETED else TransferStatus.STREAMING,
                channelId = channelId,
                speedMBps = speedMBps,
                transferredBytes = transferredBytes
            )
            _activeTransfers.value = currentList
        } else {
            // Incoming file on receiver side
            val newFile = TransferFile(
                id = fileId,
                name = "archivo_$fileId",
                sizeBytes = transferredBytes.coerceAtLeast(1L),
                mimeType = "application/octet-stream",
                progress = progress,
                status = if (progress >= 1f) TransferStatus.COMPLETED else TransferStatus.STREAMING,
                channelId = channelId,
                speedMBps = speedMBps,
                transferredBytes = transferredBytes
            )
            currentList.add(newFile)
            _activeTransfers.value = currentList
        }

        // Dual channel telemetry
        val ch1File = currentList.firstOrNull { it.channelId == 1 && it.status == TransferStatus.STREAMING }
        val ch2File = currentList.firstOrNull { it.channelId == 2 && it.status == TransferStatus.STREAMING }

        val activeStreamsCount = (if (ch1File != null) 1 else 0) + (if (ch2File != null) 1 else 0)
        val aggSpeed = (ch1File?.speedMBps ?: 0f) + (ch2File?.speedMBps ?: 0f)

        _dualChannelStats.value = DualChannelStats(
            channel1Active = ch1File != null,
            channel1FileName = ch1File?.name ?: "",
            channel1Progress = ch1File?.progress ?: 0f,
            channel1SpeedMBps = ch1File?.speedMBps ?: 0f,
            channel2Active = ch2File != null,
            channel2FileName = ch2File?.name ?: "",
            channel2Progress = ch2File?.progress ?: 0f,
            channel2SpeedMBps = ch2File?.speedMBps ?: 0f,
            totalActiveStreams = activeStreamsCount,
            aggregateSpeedMBps = aggSpeed
        )

        val totalBytes = currentList.sumOf { it.sizeBytes }.coerceAtLeast(1L)
        val sentBytes = currentList.sumOf { it.transferredBytes }
        val ovProgress = (sentBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        _overallProgress.value = ovProgress
        _overallSpeedMBps.value = aggSpeed

        val progressPercent = (ovProgress * 100).toInt()
        val speedStr = "%.1f MB/s".format(aggSpeed)
        val summary = "Transfiriendo 2 en simultáneo ($progressPercent%)"
        TransferForegroundService.updateProgress(context, progressPercent, speedStr, summary)
    }

    private fun markFileCompleted(file: TransferFile, direction: TransferDirection, peer: String) {
        val currentList = _activeTransfers.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == file.id }
        if (index != -1) {
            currentList[index] = file.copy(
                progress = 1f,
                status = TransferStatus.COMPLETED,
                transferredBytes = file.sizeBytes
            )
            _activeTransfers.value = currentList
        }

        val historyItem = TransferHistoryItem(
            id = UUID.randomUUID().toString(),
            fileName = file.name,
            sizeBytes = file.sizeBytes,
            formattedSize = file.formattedSize,
            direction = direction,
            timestamp = System.currentTimeMillis(),
            peerName = peer,
            e2eeFingerprint = _e2eeFingerprint.value,
            isEncrypted = true,
            filePath = file.localFilePath
        )
        _historyItems.value = listOf(historyItem) + _historyItems.value
    }

    fun cancelActiveTransfer() {
        client?.cancel()
        finishTransferSession()
    }

    private fun finishTransferSession() {
        _isTransferActive.value = false
        _dualChannelStats.value = DualChannelStats()
        TransferForegroundService.stop(context)
    }

    fun clearHistory() {
        _historyItems.value = emptyList()
    }
}

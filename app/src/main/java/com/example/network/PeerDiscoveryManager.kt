package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.PeerConnectionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class PeerDiscoveryManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null

    private val _discoveredPeers = MutableStateFlow<List<PeerConnectionInfo>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerConnectionInfo>> = _discoveredPeers.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("ZapyaX_Discovery")?.apply {
                setReferenceCounted(true)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun startBroadcasting(connectionInfo: PeerConnectionInfo) {
        stopBroadcasting()
        broadcastJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                }
                val payload = "ZAPYAX_BEACON|" + connectionInfo.toQrPayload()
                val bytes = payload.toByteArray(Charsets.UTF_8)
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddress, DISCOVERY_PORT)

                while (isActive) {
                    try {
                        socket.send(packet)
                    } catch (e: Exception) {
                        // ignore network fluctuations
                    }
                    delay(2000)
                }
            } catch (e: Exception) {
                // socket closed
            } finally {
                socket?.close()
            }
        }
    }

    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    fun startListening() {
        if (listenJob != null) return
        try {
            multicastLock?.acquire()
        } catch (e: Exception) {
            // ignore
        }

        listenJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }
                val buffer = ByteArray(1024)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length, Charsets.UTF_8)

                    if (message.startsWith("ZAPYAX_BEACON|")) {
                        val qrPayload = message.removePrefix("ZAPYAX_BEACON|")
                        val peer = PeerConnectionInfo.fromQrPayload(qrPayload)
                        if (peer != null) {
                            val current = _discoveredPeers.value.toMutableList()
                            if (current.none { it.ipAddress == peer.ipAddress && it.port == peer.port }) {
                                current.add(peer)
                                _discoveredPeers.value = current
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // socket closed
            } finally {
                socket?.close()
                try {
                    if (multicastLock?.isHeld == true) multicastLock?.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        const val DISCOVERY_PORT = 8992
    }
}

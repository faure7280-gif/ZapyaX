package com.example.network

import java.io.DataInputStream
import java.io.DataOutputStream

object TransferProtocol {
    const val MAGIC: String = "ZBEAM"
    const val PROTOCOL_VERSION: Int = 1

    const val MSG_HANDSHAKE: Byte = 0x01
    const val MSG_FILE_HEADER: Byte = 0x02
    const val MSG_CHUNK_DATA: Byte = 0x03
    const val MSG_FILE_END: Byte = 0x04
    const val MSG_SESSION_CLOSE: Byte = 0x05

    const val CHUNK_SIZE = 64 * 1024 // 64 KB buffer for high-speed streaming

    fun writeHandshake(
        out: DataOutputStream,
        sessionId: String,
        channelId: Int,
        deviceName: String
    ) {
        out.writeUTF(MAGIC)
        out.writeInt(PROTOCOL_VERSION)
        out.writeByte(MSG_HANDSHAKE.toInt())
        out.writeUTF(sessionId)
        out.writeInt(channelId)
        out.writeUTF(deviceName)
        out.flush()
    }

    fun readHandshake(din: DataInputStream): HandshakeData {
        val magic = din.readUTF()
        require(magic == MAGIC) { "Protocolo inválido: magic no coincide" }
        val version = din.readInt()
        val msgType = din.readByte()
        require(msgType == MSG_HANDSHAKE) { "Se esperaba MSG_HANDSHAKE" }
        val sessionId = din.readUTF()
        val channelId = din.readInt()
        val deviceName = din.readUTF()
        return HandshakeData(version, sessionId, channelId, deviceName)
    }

    data class HandshakeData(
        val version: Int,
        val sessionId: String,
        val channelId: Int,
        val deviceName: String
    )
}

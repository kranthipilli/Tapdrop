package com.tapdrop.handoff

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class WifiDirectTransferManager(
    private val context: Context,
) {
    private var callback: ((String, Map<String, Any?>) -> Unit)? = null
    private val io = Executors.newSingleThreadExecutor()

    fun setEventCallback(callback: (String, Map<String, Any?>) -> Unit) {
        this.callback = callback
    }

    fun initialize() = Unit

    fun confirmPeer(sessionId: String) = Unit

    fun startNegotiation(sessionId: String) {
        // Hook WifiP2pManager discovery + connection logic here.
        callback?.invoke("onConnectionEstablished", mapOf("ip" to "192.168.49.1", "port" to 8988, "sessionId" to sessionId))
    }

    fun startTransfer(sessionId: String, filePath: String) {
        io.execute {
            try {
                val file = File(filePath)
                val total = file.length()
                var sent = 0L

                FileInputStream(file).use { input ->
                    val socket = Socket("192.168.49.1", 8988)
                    socket.getOutputStream().use { out ->
                        val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
                        val iv = ByteArray(12) { 1 }
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            val encrypted = cipher.update(buffer, 0, read) ?: ByteArray(0)
                            out.write(encrypted)
                            sent += read
                            callback?.invoke("onTransferProgress", mapOf("bytesSent" to sent, "total" to total, "sessionId" to sessionId))
                        }
                        val finalChunk = cipher.doFinal()
                        out.write(finalChunk)
                    }
                    socket.close()
                }
                callback?.invoke("onTransferCompleted", mapOf("sessionId" to sessionId))
            } catch (e: Exception) {
                callback?.invoke("onTransferFailed", mapOf("errorCode" to "TRANSFER_ERROR", "message" to (e.message ?: "unknown"), "sessionId" to sessionId))
            }
        }
    }

    fun startServer() {
        io.execute {
            ServerSocket(8988).use { server ->
                val client = server.accept()
                client.close()
            }
        }
    }

    fun cancelTransfer(sessionId: String) {
        callback?.invoke("onTransferFailed", mapOf("errorCode" to "USER_CANCEL", "sessionId" to sessionId))
    }
}

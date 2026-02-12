package com.tapdrop.handoff

import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class TapdropChannelBridge(
    private val nfcManager: NfcHandoffManager,
    private val wifiDirectManager: WifiDirectTransferManager,
) : MethodChannel.MethodCallHandler {

    private var events: EventChannel.EventSink? = null

    fun attachEventSink(sink: EventChannel.EventSink?) {
        events = sink
        nfcManager.setEventCallback(::emit)
        wifiDirectManager.setEventCallback(::emit)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                nfcManager.initialize()
                wifiDirectManager.initialize()
                result.success(true)
            }
            "enableForegroundDispatch" -> {
                nfcManager.enableForegroundDispatch()
                result.success(true)
            }
            "disableForegroundDispatch" -> {
                nfcManager.disableForegroundDispatch()
                result.success(true)
            }
            "confirmPeer" -> {
                val sessionId = call.argument<String>("sessionId") ?: return result.error("ARG", "Missing sessionId", null)
                wifiDirectManager.confirmPeer(sessionId)
                emit("onPeerConfirmed", mapOf("sessionId" to sessionId))
                result.success(true)
            }
            "startWifiDirectNegotiation" -> {
                val sessionId = call.argument<String>("sessionId") ?: return result.error("ARG", "Missing sessionId", null)
                wifiDirectManager.startNegotiation(sessionId)
                result.success(true)
            }
            "startTransfer" -> {
                val sessionId = call.argument<String>("sessionId") ?: return result.error("ARG", "Missing sessionId", null)
                val filePath = call.argument<String>("filePath") ?: return result.error("ARG", "Missing filePath", null)
                wifiDirectManager.startTransfer(sessionId, filePath)
                result.success(true)
            }
            "cancelTransfer" -> {
                val sessionId = call.argument<String>("sessionId") ?: return result.error("ARG", "Missing sessionId", null)
                wifiDirectManager.cancelTransfer(sessionId)
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    private fun emit(type: String, payload: Map<String, Any?> = emptyMap()) {
        events?.success(mapOf("event" to type, "data" to payload))
    }
}

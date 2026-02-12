package com.tapdrop.handoff

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Parcelable
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.UUID

class NfcHandoffManager(
    private val activity: Activity,
) {
    private var callback: ((String, Map<String, Any?>) -> Unit)? = null
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val replayCache = LinkedHashSet<String>()

    fun setEventCallback(callback: (String, Map<String, Any?>) -> Unit) {
        this.callback = callback
    }

    fun initialize() = Unit

    fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun onNewIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return
        val record = firstRecord(messages) ?: return
        val json = decodeTextRecord(record) ?: return
        val payload = JSONObject(json)
        if (!isFresh(payload)) return
        callback?.invoke("onNfcDetected", payload.toMap())
    }

    fun buildOutgoingNdef(deviceId: UUID, appInstanceId: UUID, pubKeyBase64: String): NdefMessage {
        val payload = JSONObject()
            .put("v", 1)
            .put("deviceId", deviceId.toString())
            .put("appInstanceId", appInstanceId.toString())
            .put("sessionId", UUID.randomUUID().toString())
            .put("ephemeralPubKey", pubKeyBase64)
            .put("preferredTransfer", "WIFI_DIRECT")
            .put("timestampMs", System.currentTimeMillis())
            .put("nonce", UUID.randomUUID().toString())
            .toString()
        val lang = "en".toByteArray(Charsets.US_ASCII)
        val text = payload.toByteArray(Charsets.UTF_8)
        val data = ByteArray(1 + lang.size + text.size)
        data[0] = lang.size.toByte()
        System.arraycopy(lang, 0, data, 1, lang.size)
        System.arraycopy(text, 0, data, 1 + lang.size, text.size)
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
        return NdefMessage(arrayOf(record))
    }

    private fun firstRecord(messages: Array<Parcelable>): NdefRecord? {
        if (messages.isEmpty()) return null
        val msg = messages[0] as? NdefMessage ?: return null
        return msg.records.firstOrNull()
    }

    private fun decodeTextRecord(record: NdefRecord): String? {
        val payload = record.payload ?: return null
        if (payload.isEmpty()) return null
        val languageLength = payload[0].toInt() and 0x3F
        return String(payload, 1 + languageLength, payload.size - 1 - languageLength, Charset.forName("UTF-8"))
    }

    private fun isFresh(payload: JSONObject): Boolean {
        val timestamp = payload.optLong("timestampMs", 0)
        val nonce = payload.optString("nonce", "")
        val deviceId = payload.optString("deviceId", "")
        if (timestamp == 0L || nonce.isEmpty() || deviceId.isEmpty()) return false
        if (kotlin.math.abs(System.currentTimeMillis() - timestamp) > 30_000L) return false
        val replayKey = "$deviceId:$nonce"
        if (replayCache.contains(replayKey)) return false
        replayCache.add(replayKey)
        if (replayCache.size > 512) replayCache.remove(replayCache.first())
        return true
    }
}

private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { get(it) }

# Tapdrop Android Hardware Implementation Plan (FlutterFlow + Kotlin)

## 1) Runtime Architecture

- FlutterFlow renders UX, state machine, consent UI, and transfer progress.
- A native Android Kotlin layer owns NFC, Wi-Fi Direct, socket transport, and encryption.
- Flutter communicates with Kotlin through one `MethodChannel` (`tapdrop/control`) and one `EventChannel` (`tapdrop/events`).

## 2) Mandatory Platform Channel Contract

**EventChannel events (native -> Flutter):**

- `onNfcDetected(payload)`
- `onPeerConfirmed()`
- `onConnectionEstablished(ip, port)`
- `onTransferProgress(bytesSent, total)`
- `onTransferCompleted()`
- `onTransferFailed(errorCode)`

**MethodChannel calls (Flutter -> native):**

- `initialize()`
- `enableForegroundDispatch()`
- `disableForegroundDispatch()`
- `confirmPeer(sessionId)`
- `startWifiDirectNegotiation(sessionId)`
- `startTransfer(filePath, sessionId)`
- `cancelTransfer(sessionId)`

## 3) NFC Hardware Flow (Foreground Dispatch)

1. Activity starts and calls `enableForegroundDispatch()` in `onResume`.
2. User physically taps two NFC-capable Android phones.
3. Android delivers `ACTION_NDEF_DISCOVERED` to `onNewIntent()`.
4. Native code parses NDEF payload and validates:
   - timestamp window (for anti-replay)
   - required fields
   - payload signature / schema version
5. Native emits `onNfcDetected(payload)` to Flutter.
6. FlutterFlow shows NameDrop-style accept screen.
7. Once both sides accept, Flutter calls `confirmPeer(sessionId)`.
8. Native emits `onPeerConfirmed()` and starts Wi-Fi Direct negotiation.

## 4) NFC Payload Schema (JSON inside NDEF Text record)

```json
{
  "v": 1,
  "deviceId": "<uuid>",
  "appInstanceId": "<uuid>",
  "sessionId": "<uuid>",
  "ephemeralPubKey": "<base64-x25519-pub>",
  "preferredTransfer": "WIFI_DIRECT",
  "timestampMs": 1730000000000,
  "nonce": "<base64-12b>"
}
```

Validation rules:

- Reject if `abs(now - timestampMs) > 30_000`.
- Reject if `(deviceId, nonce)` exists in replay cache.
- Reject unknown schema version.

## 5) Wi-Fi Direct + TCP Transfer

- Use `WifiP2pManager` + `Channel` for peer formation.
- Group Owner opens `ServerSocket` on fixed port (e.g., `8988`).
- Client connects via `Socket(groupOwnerIp, 8988)`.
- File transfer protocol:
  1. Send encrypted transfer header (`sessionId`, filename, size, mime).
  2. Send chunked stream (`64 KiB` chunks).
  3. Every chunk includes index + length + MAC.
  4. Receiver ACKs every N chunks for resume checkpoints.
- Resume support stores `lastAckedChunk` in local DB.

## 6) Encryption Model

- Generate per-session X25519 key pair on NFC exchange.
- Derive shared secret via ECDH.
- Derive AES-256 key using HKDF-SHA256.
- Encrypt payload stream with AES-GCM (unique nonce per chunk).
- Destroy session key material on completion/failure/cancel.

## 7) Android Permissions

Manifest + runtime handling required for:

- `android.permission.NFC`
- `android.permission.NEARBY_WIFI_DEVICES` (Android 13+)
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.BLUETOOTH_SCAN`
- `android.permission.ACCESS_WIFI_STATE`
- `android.permission.CHANGE_WIFI_STATE`
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.READ_MEDIA_VIDEO`
- `android.permission.READ_MEDIA_AUDIO`
- `android.permission.FOREGROUND_SERVICE`

## 8) Hardware Test Matrix (must be executed on real phones)

- NFC tap success (multiple orientations)
- NFC tap + move away before consent
- Wi-Fi Direct formation timeout
- Mid-transfer disconnect + resume
- Screen lock/unlock mid-transfer
- 1GB+ file transfer throughput and integrity check
- Cross-OEM pairings (Samsung/Pixel/Xiaomi preferred)

## 9) Delivery Checklist

- FlutterFlow project with custom actions wired to platform channels.
- Native Kotlin module integrated in Android folder.
- APK built and installed on at least two NFC-capable devices.
- Test report with timestamps, logs, and transfer metrics.

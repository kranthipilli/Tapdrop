# Tapdrop (Android, FlutterFlow + Kotlin)

Real NFC-triggered peer discovery and offline file transfer stack for Android.

## Included in this repository

- `docs/hardware_implementation_plan.md`: end-to-end implementation and test plan.
- `native-android/src/main/kotlin/com/tapdrop/handoff/*`: Kotlin bridge + NFC + transfer manager scaffolds.
- `flutter/lib/tapdrop_native_bridge.dart`: Flutter-side channel client API.

## Required Android manifest permissions

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## Integration order

1. Register platform channels in your Flutter Android host activity.
2. Forward `onNewIntent(intent)` to `NfcHandoffManager.onNewIntent(intent)`.
3. Call `enableForegroundDispatch()` in `onResume` and `disableForegroundDispatch()` in `onPause`.
4. Wire FlutterFlow custom actions to `TapdropNativeBridge` methods.
5. Replace placeholder socket peer IP/keying with negotiated Wi-Fi Direct + ECDH session material.
6. Validate with two physical NFC-enabled Android devices.

## Build on Samsung S23

See `docs/build_apk_samsung_s23.md` for exact setup, host integration, build, and install steps for a real APK on Samsung S23.

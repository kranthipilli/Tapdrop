# Build + Install APK on Samsung S23 (Android 14) for Tapdrop

This repository currently contains the native/Flutter bridge scaffolding only. To install on a Samsung S23, place this code into a real Flutter project (or FlutterFlow exported project), wire Android host integration, then build/install.

## 1) Prerequisites (host machine)

- Android Studio Hedgehog+ with Android SDK 34
- Java 17
- Flutter stable (3.22+ recommended)
- Samsung USB Driver (Windows) or Android File Transfer/ADB tools (macOS/Linux)
- USB cable + Samsung S23 with Developer Mode enabled

Verify tools:

```bash
flutter --version
adb version
java -version
```

## 2) Get a Flutter project to host this code

You need one of:

- **FlutterFlow export** (`Download Code`) OR
- A plain Flutter app (`flutter create tapdrop_app`)

If using plain Flutter:

```bash
flutter create tapdrop_app
cd tapdrop_app
```

Then copy in:

- `flutter/lib/tapdrop_native_bridge.dart` -> `lib/tapdrop_native_bridge.dart`
- `native-android/src/main/kotlin/com/tapdrop/handoff/*` -> `android/app/src/main/kotlin/com/tapdrop/handoff/*`

## 3) AndroidManifest permissions + NFC feature

In `android/app/src/main/AndroidManifest.xml` add:

```xml
<uses-feature android:name="android.hardware.nfc" android:required="true" />
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Add NFC intent filter in the `<activity>` for your main activity:

```xml
<intent-filter>
    <action android:name="android.nfc.action.NDEF_DISCOVERED" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

## 4) Register platform channels in MainActivity (Kotlin)

In `android/app/src/main/kotlin/.../MainActivity.kt`:

- Instantiate `NfcHandoffManager`, `WifiDirectTransferManager`, `TapdropChannelBridge`
- Register:
  - `MethodChannel("tapdrop/control")`
  - `EventChannel("tapdrop/events")`
- Forward `onNewIntent(intent)` to `nfcManager.onNewIntent(intent)`
- Call `enableForegroundDispatch()` in `onResume` and `disableForegroundDispatch()` in `onPause`

Without this, APK builds but handoff won’t work.

## 5) Runtime permissions (Android 13/14)

Request at runtime before starting discovery/transfer:

- Nearby devices (Wi-Fi/Bluetooth)
- Location (for Wi-Fi Direct discovery on many OEMs)
- Media read permissions

If permissions are denied, show a blocking error in FlutterFlow UI and deep-link to app settings.

## 6) Build debug APK

From Flutter project root:

```bash
flutter clean
flutter pub get
flutter build apk --debug
```

Output:

- `build/app/outputs/flutter-apk/app-debug.apk`

## 7) Connect Samsung S23 and install

On phone:

1. Settings -> About phone -> Software information -> tap Build number 7x
2. Settings -> Developer options -> enable **USB debugging**
3. Plug USB and accept RSA fingerprint prompt

Install with ADB:

```bash
adb devices
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

Launch:

```bash
adb shell monkey -p <your.package.name> -c android.intent.category.LAUNCHER 1
```

## 8) Build release APK (shareable)

Create upload keystore:

```bash
keytool -genkey -v -keystore ~/tapdrop-upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tapdrop
```

Configure in Flutter project:

- `android/key.properties`
- `android/app/build.gradle` signing config

Then:

```bash
flutter build apk --release
```

Output:

- `build/app/outputs/flutter-apk/app-release.apk`

Install:

```bash
adb install -r build/app/outputs/flutter-apk/app-release.apk
```

## 9) Samsung S23 hardware checklist (must pass)

- NFC is ON on both phones
- App is foregrounded on both phones
- Physical tap triggers NFC event and accept UI
- Accept on both devices starts connection
- Transfer progress updates in UI
- 1GB+ file transfer completes and file hash matches

## 10) Common Samsung-specific issues

- **NFC event never fires**: verify intent filter + foreground dispatch + app in foreground.
- **Wi-Fi Direct discovery empty**: ensure location permission granted and Wi-Fi enabled.
- **Install blocked**: allow installs via USB/ADB; keep package signature consistent with `-r` reinstall.
- **Background kill**: exclude app from battery optimization during long transfer tests.

import 'dart:async';

import 'package:flutter/services.dart';

class TapdropNativeBridge {
  static const MethodChannel _control = MethodChannel('tapdrop/control');
  static const EventChannel _events = EventChannel('tapdrop/events');

  Stream<Map<String, dynamic>> get events =>
      _events.receiveBroadcastStream().map((event) => Map<String, dynamic>.from(event));

  Future<void> initialize() => _control.invokeMethod('initialize');

  Future<void> enableForegroundDispatch() =>
      _control.invokeMethod('enableForegroundDispatch');

  Future<void> disableForegroundDispatch() =>
      _control.invokeMethod('disableForegroundDispatch');

  Future<void> confirmPeer(String sessionId) =>
      _control.invokeMethod('confirmPeer', {'sessionId': sessionId});

  Future<void> startWifiDirectNegotiation(String sessionId) =>
      _control.invokeMethod('startWifiDirectNegotiation', {'sessionId': sessionId});

  Future<void> startTransfer({required String filePath, required String sessionId}) =>
      _control.invokeMethod('startTransfer', {'filePath': filePath, 'sessionId': sessionId});

  Future<void> cancelTransfer(String sessionId) =>
      _control.invokeMethod('cancelTransfer', {'sessionId': sessionId});
}

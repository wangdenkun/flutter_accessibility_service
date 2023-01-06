import 'dart:async';
import 'dart:developer';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_accessibility_service/accessibility_event.dart';

class FlutterAccessibilityService {
  FlutterAccessibilityService._();

  static const MethodChannel _methodeChannel = MethodChannel('x-slayer/accessibility_channel');
  static const EventChannel _eventChannel = EventChannel('x-slayer/accessibility_event');
  static Stream<AccessibilityEvent>? _stream;
  static Stream<Map<String,dynamic>>? _eventStream;


  /// stream the incoming Accessibility events
  static Stream<AccessibilityEvent> get accessStream {
    if (Platform.isAndroid) {
      _stream ??= _eventChannel.receiveBroadcastStream().map<AccessibilityEvent>((event) => AccessibilityEvent.fromMap(event),);
      return _stream!;
    }
    throw Exception("Accessibility API exclusively available on Android!");
  }
 /// stream the incoming Accessibility events
  static Stream<Map<String,dynamic>> get eventStream {
    if (Platform.isAndroid) {
      _eventStream ??= _eventChannel.receiveBroadcastStream().map<Map<String,dynamic>>((event) => Map.from(event));
      return _eventStream!;
    }
    throw Exception("Accessibility API exclusively available on Android!");
  }

  /// request accessibility permission
  /// it will open the accessibility settings page and return `true` once the permission granted.
  static Future<bool> requestAccessibilityPermission() async {
    try {
      return await _methodeChannel.invokeMethod('requestAccessibilityPermission');
    } on PlatformException catch (error) {
      log("$error");
      return Future.value(false);
    }
  }

  /// check if accessibility permession is enebaled
  static Future<bool> isAccessibilityPermissionEnabled() async {
    try {
      return await _methodeChannel.invokeMethod('isAccessibilityPermissionEnabled');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  /// 理论上将 不应该手动开启服务 应该是给了权限之后 由系统自动开启服务
  @Deprecated('理论上将 不应该手动开启服务 应该是给了权限之后 由系统自动开启服务')
  static Future<bool> startService() async {
    try {
      return await _methodeChannel.invokeMethod('startService');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  static Future<bool> isServiceRunning() async {
    try {
      return await _methodeChannel.invokeMethod('checkServiceIsRunning');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }
  static Future<bool> performGlobalAction({required String actionType}) async {
    try {
      return await _methodeChannel.invokeMethod('performGlobalAction',{
        'actionType': actionType,
      });
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }
}

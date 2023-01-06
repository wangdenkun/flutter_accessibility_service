# flutter_accessibility_service

### 运行环境
* flutter2.10.5
### 关于用AS运行example报错
Didn't find class "androidx.window.sidecar.SidecarInterface$SidecarCallback"  
参考[flutter issue](https://github.com/flutter/flutter/issues/99404)还是没能修复，但是好像不影响flutter工程的运行

### 新增特性
* 获取service运行状态(注意 和权限两码事，虽然有权限但是服务也不一定正常运行 有可能被系统干掉)
* 获取无障碍访问权限后，可以实现"三大金刚键"功能。
* 可以提前在flutter侧注册监听以便获取service运行状态(如影设备进入授权页面后 无法通过返回按钮返回到应用，可以在得知服务成功运行消息后处理旋钮事件)

a plugin for interacting with Accessibility Service in Android.

Accessibility services are intended to assist users with disabilities in using Android devices and apps, or I can say to get android os events like keyboard key press events or notification received events etc.

for more info check [Accessibility Service](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)

### Installation and usage ###

Add package to your pubspec:

```yaml
dependencies:
  flutter_accessibility_service: any # or the latest version on Pub
```

Inside AndroidManifest add this to bind your accessibility service with your application

```xml
    .
    .
    <service android:name="slayer.accessibility.service.flutter_accessibility_service.AccessibilityListener"
                android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" android:exported="false">
      <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
      </intent-filter>
      <meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibilityservice" />
    </service>
    .
    .
</application>

```

Create Accesiblity config file named `accessibilityservice.xml` inside `res/xml` and add the following code inside it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowsChanged|typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackVisual"
    android:notificationTimeout="300"
    android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews|flagRequestTouchExplorationMode|flagRequestEnhancedWebAccessibility|flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
>
</accessibility-service>

```

### USAGE


```dart
 /// check if accessibility permission is enebaled
 final bool status = await FlutterAccessibilityService.isAccessibilityPermissionEnabled();
 
 /// request accessibility permission
 /// it will open the accessibility settings page and return `true` once the permission granted.
 final bool status = await FlutterAccessibilityService.requestAccessibilityPermission();
 
 /// stream the incoming Accessibility events
  FlutterAccessibilityService.accessStream.listen((event) {
    log("Current Event: $event");
  
  /*
  Current Event: AccessibilityEvent: (
     Action Type: 0 
     Event Time: 2022-04-11 14:19:56.556834 
     Package Name: com.facebook.katana 
     Event Type: EventType.typeWindowContentChanged 
     Captured Text: events you may like 
     content Change Types: ContentChangeTypes.contentChangeTypeSubtree 
     Movement Granularity: 0
     Is Active: true
     is focused: true
     in Pip: false
     window Type: WindowType.typeApplication
     Screen bounds: left: 0 - right: 720 - top: 0 - bottom: 1544 - width: 720 - height: 1544
)
  */
  
  });
```

The `AccessibilityEvent` provides:

```dart
  /// the performed action that triggered this event
  int? actionType;
  
  /// the time in which this event was sent.
  DateTime? eventTime;
  
  /// the package name of the source
  String? packageName;
  
  /// the event type.
  EventType? eventType;
  
  /// Gets the text of this node.
  String? capturedText;
  
  /// the bit mask of change types signaled by a `TYPE_WINDOW_CONTENT_CHANGED` event or `TYPE_WINDOW_STATE_CHANGED`. A single event may represent multiple change types
  ContentChangeTypes? contentChangeTypes;
  
  /// the movement granularity that was traversed
  int? movementGranularity;
  
  /// the type of the window
  WindowType? windowType;
  
  /// check if this window is active. An active window is the one the user is currently touching or the window has input focus and the user is not touching any window.
  bool? isActive;
  
  /// check if this window has input focus.
  bool? isFocused;
  
  /// Check if the window is in picture-in-picture mode.
  bool? isPip;

  /// Gets the node bounds in screen coordinates.
  ScreenBounds? screenBounds;
```

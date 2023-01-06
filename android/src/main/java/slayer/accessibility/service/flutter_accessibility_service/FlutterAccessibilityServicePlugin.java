package slayer.accessibility.service.flutter_accessibility_service;

import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterAccessibilityServicePlugin
 */
public class FlutterAccessibilityServicePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {

    private static final String CHANNEL_TAG = "x-slayer/accessibility_channel";
    private static final String EVENT_TAG = "x-slayer/accessibility_event";

    private MethodChannel channel;
    private AccessibilityReceiver accessibilityReceiver;
    private EventChannel eventChannel;
    private Context context;
    private Activity mActivity;

//    private Result pendingResult;
    private Result pendingResultOfAccessibility;
    static final int REQUEST_CODE_FOR_ACCESSIBILITY = 167;
    
    private final String TAG = FlutterAccessibilityServicePlugin.class.getSimpleName();

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_TAG);
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_TAG);
        eventChannel.setStreamHandler(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AccessibilityListener.ACCESSIBILITY_INTENT);
        accessibilityReceiver = new AccessibilityReceiver(null,this);
        context.registerReceiver(accessibilityReceiver, intentFilter);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("isAccessibilityPermissionEnabled")) {
            result.success(Utils.isAccessibilitySettingsOn(context));
            return;
        }
        if (call.method.equals("requestAccessibilityPermission")) {
            if (checkServiceIsRunning()){
                result.success(true);
                return;
            }
            pendingResultOfAccessibility = result;
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_ACCESSIBILITY);
            return;
        }
        if (call.method.equals("startService")){
            boolean res = startService();
            result.success(res);
            return;
        }
        if (call.method.equals("checkServiceIsRunning")){
            boolean res = checkServiceIsRunning();
            result.success(res);
            return;
        }
        if (call.method.equals("performGlobalAction")){
            if (AccessibilityListener.getInstance() !=  null){
                if (call.hasArgument("actionType")){
                    String actionType = call.argument("actionType");
                    if (actionType == null){
                        result.success(false);
                        return;
                    }
                    if (actionType.equals("GLOBAL_ACTION_HOME")){
                        AccessibilityListener.getInstance().performGlobalAction(GLOBAL_ACTION_HOME);
                        result.success(true);
                        return;
                    }
                    if (actionType.equals("GLOBAL_ACTION_RECENTS")){
                        AccessibilityListener.getInstance().performGlobalAction(GLOBAL_ACTION_RECENTS);
                        result.success(true);
                        return;
                    }
                    if (actionType.equals("GLOBAL_ACTION_BACK")){
                        AccessibilityListener.getInstance().performGlobalAction(GLOBAL_ACTION_BACK);
                        result.success(true);
                        return;
                    }
                }
            }
            result.success(false);
            return;
        }
        result.notImplemented();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        if (accessibilityReceiver != null){
            accessibilityReceiver.eventSink = events;
        }
    }

    public boolean startService(){
        if (checkServiceIsRunning()){
            return true;
        }
        /// Set up listener intent
        try{
            Intent listenerIntent = new Intent(context, AccessibilityListener.class);
            context.startService(listenerIntent);
            Log.i("AccessibilityPlugin", "Started the accessibility tracking service.");
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public void onCancel(Object arguments) {
        context.unregisterReceiver(accessibilityReceiver);
        accessibilityReceiver = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_ACCESSIBILITY) {
            if (pendingResultOfAccessibility == null) return true;
            if (resultCode == Activity.RESULT_OK) {
                pendingResultOfAccessibility.success(true);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                pendingResultOfAccessibility.success(Utils.isAccessibilitySettingsOn(context));
            } else {
                pendingResultOfAccessibility.success(false);
            }
            pendingResultOfAccessibility = null;
            return true;
        }
        return false;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.mActivity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.mActivity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        this.mActivity = null;
    }

    boolean checkServiceIsRunning(){
        ActivityManager am = (ActivityManager) this.mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = am.getRunningServices(1000);
        String id = null;
        boolean isServiceOpen = false;
        for (ActivityManager.RunningServiceInfo info : serviceInfoList) {
            id = info.service.getClassName();
            if (id.contains(AccessibilityListener.class.getName())) {
                isServiceOpen = true;
                break;
            }
        }
        if (!isServiceOpen) {
            Log.d(TAG, "checkServiceIsRunning:  无障碍访问功能未开启!");
            return false;
        }
        return true;
    }


//    @RequiresApi(api = Build.VERSION_CODES.M)
//    private boolean checkOverlayPermissions(){
//        return Settings.canDrawOverlays(context);
//    }
//
//    @RequiresApi(Build.VERSION_CODES.M)
//    private void requestOverlayPermissions(){
//        mActivity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mActivity.getPackageName())));
//    }
}

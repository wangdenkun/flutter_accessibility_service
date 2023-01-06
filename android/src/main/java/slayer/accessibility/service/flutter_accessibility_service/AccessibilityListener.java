package slayer.accessibility.service.flutter_accessibility_service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.HashMap;
import java.util.List;
import android.os.Handler;


public class AccessibilityListener extends AccessibilityService {

    private static AccessibilityListener accessibilityListener;

    public static String ACCESSIBILITY_INTENT = "accessibility_event";
    public static String ACCESSIBILITY_NAME = "packageName";
    public static String ACCESSIBILITY_TEXT = "capturedText";
    public static String ACCESSIBILITY_ACTION = "action";
    public static String ACCESSIBILITY_EVENT_TIME = "eventTime";
    public static String ACCESSIBILITY_CHANGES_TYPES = "contentChangeTypes";
    public static String ACCESSIBILITY_MOVEMENT = "movementGranularity";
    public static String ACCESSIBILITY_IS_ACTIVE = "isActive";
    public static String ACCESSIBILITY_IS_FOCUSED = "isFocused";
    public static String ACCESSIBILITY_IS_PIP = "isInPictureInPictureMode";
    public static String ACCESSIBILITY_WINDOW_TYPE = "windowType";
    public static String ACCESSIBILITY_SCREEN_BOUNDS = "screenBounds";
    public static String ACCESSIBILITY_NODES_TEXT = "nodesText";

    public static String ACCESSIBILITY_EVENT_TYPE = "eventType";
    public static String ACCESSIBILITY_KEY_CODE_TYPE = "keyCodeType";
    public static String ACCESSIBILITY_KEY_CODE_ACTION_TYPE = "keyCodeActionType";

    public String TAG = "--->" + AccessibilityListener.class.getSimpleName();


    public static AccessibilityListener getInstance(){
        return accessibilityListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }

    /**
     *  这个地方调用了 才说明真正起作用了
     *  插件申请权限的整体回调流程：
     *  FlutterAccessibilityServicePlugin.requestAccessibilityPermission ->
     *  AccessibilityListener.onServiceConnected send  'serviceStarted' event ->
     *  AccessibilityReceiver.onReceive get 'serviceStarted' event->
     *  FlutterAccessibilityServicePlugin.onActivityResult
      */
    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "onServiceConnected");
        accessibilityListener = this;
        super.onServiceConnected();
//        Intent intent = new Intent(ACCESSIBILITY_INTENT);
//        intent.putExtra(ACCESSIBILITY_EVENT_TYPE,"serviceStarted");
//        intent.putExtra(ACCESSIBILITY_KEY_CODE_TYPE,"");
//        intent.putExtra(ACCESSIBILITY_KEY_CODE_ACTION_TYPE,"");
//        sendBroadcast(intent);
        sendEventDataToReceiver("serviceStarted","","");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind " + intent.toString());
        accessibilityListener = null;
        sendEventDataToReceiver("serviceStopped","","");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    CheckForLongPress mPendingCheckForLongPress = null;
    CheckForDoublePress mPendingCheckForDoublePress = null;
    Handler mHandler = new Handler();
    private int currentKeyCode = 0;
    private static Boolean isDoubleClick = false;
    private static Boolean isLongClick = false;

    //========= 旋钮事件处理 =========
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keycode = event.getKeyCode();
        // 只关注需要管理旋钮的三种事件
        if (keycode != KeyEvent.KEYCODE_DPAD_UP && keycode != KeyEvent.KEYCODE_DPAD_CENTER && keycode != KeyEvent.KEYCODE_DPAD_DOWN) {
            return super.onKeyEvent(event);
        }
        // 有不同按键按下，取消长按、短按的判断
        if (currentKeyCode != keycode) {
            removeLongPressCallback();
            isDoubleClick = false;
        }
        // 处理长按、单击、双击按键
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // 左旋或右旋 不进行长按、双击的处理
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN){
                singleClick(event.getKeyCode());
            }else{
                checkForLongClick(event);
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            checkForDoubleClick(event);
        }
        // 拦截事件 不让系统传给app 防止和flutter应用的无障碍访问联动
        return true;
//        return super.onKeyEvent(event);
    }

    private void longPress(int keycode) {
        // 只有按下按钮才算长按
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER){
            Log.i(TAG, "doublePress 长按旋钮 " + keycode);
            sendEventDataToReceiver("keyCode","KEYCODE_DPAD_CENTER","longPress");
        }
    }
    private void singleClick(int keycode) {
        // 向左 向右 按下 都可能是单击
        String keyCodeType = "";
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) keyCodeType = "KEYCODE_DPAD_CENTER";
        if (keycode == KeyEvent.KEYCODE_DPAD_UP) keyCodeType = "KEYCODE_DPAD_UP";
        if (keycode == KeyEvent.KEYCODE_DPAD_DOWN) keyCodeType = "KEYCODE_DPAD_DOWN";
        Log.i(TAG, "singleClick 单击事件 " + keycode);
        sendEventDataToReceiver("keyCode",keyCodeType,"singleClick");
    }
    private void doublePress(int keycode) {
        //  只有快速的两次按下才算双击
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER){
            Log.i(TAG, "doublePress 双击旋钮 " + keycode);
            sendEventDataToReceiver("keyCode","KEYCODE_DPAD_CENTER","doublePress");
        }
    }

    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
            mHandler.removeCallbacks(mPendingCheckForLongPress);
        }
    }
    private void checkForLongClick(KeyEvent event) {
        int count = event.getRepeatCount();
        int keycode = event.getKeyCode();
        if (count == 0) {
            currentKeyCode = keycode;
        } else {
            return;
        }
        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.setKeycode(event.getKeyCode());
        mHandler.postDelayed(mPendingCheckForLongPress, 1000);
    }
    private void checkForDoubleClick(KeyEvent event) {
        // 有长按时间发生，则不处理单击、双击事件
        removeLongPressCallback();
        if (isLongClick) {
            isLongClick = false;
            return;
        }

        if (!isDoubleClick) {
            isDoubleClick = true;
            if (mPendingCheckForDoublePress == null) {
                mPendingCheckForDoublePress = new CheckForDoublePress();
            }
            mPendingCheckForDoublePress.setKeycode(event.getKeyCode());
            mHandler.postDelayed(mPendingCheckForDoublePress, 500);
        } else {
            // 500ms内两次单击，触发双击
            isDoubleClick = false;
            doublePress(event.getKeyCode());
        }
    }
    class CheckForDoublePress implements Runnable {

        int currentKeycode = 0;

        public void run() {
            if (isDoubleClick) {
                singleClick(currentKeycode);
            }
            isDoubleClick = false;
        }

        public void setKeycode(int keycode) {
            currentKeycode = keycode;
        }
    }
    class CheckForLongPress implements Runnable {

        int currentKeycode = 0;

        public void run() {
            isLongClick = true;
            longPress(currentKeycode);
        }

        public void setKeycode(int keycode) {
            currentKeycode = keycode;
        }
    }

    /**
     * service拦截到的keyEvent -> AccessibilityReceiver -> flutter platform channel -> flutter
     * @param eventType 事件类型
     * @param codeType  KeyEvent类型
     * @param actionType KeyEvent操作类型
     */
    private void sendEventDataToReceiver(String eventType,String codeType,String actionType){
        Intent intent = new Intent(ACCESSIBILITY_INTENT);
        intent.putExtra(ACCESSIBILITY_EVENT_TYPE,eventType);
        intent.putExtra(ACCESSIBILITY_KEY_CODE_TYPE,codeType);
        intent.putExtra(ACCESSIBILITY_KEY_CODE_ACTION_TYPE,actionType);
        sendBroadcast(intent);
    }
    //========= 无障碍访问功能 暂时用不到 =========
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
//
//        final int eventType = accessibilityEvent.getEventType();
//        AccessibilityNodeInfo parentNodeInfo = accessibilityEvent.getSource();
//        AccessibilityWindowInfo windowInfo = null;
//        List<String> nextTexts = new ArrayList<>();
//
//
//        if (parentNodeInfo == null) {
//            return;
//        }
//
//        String packageName = parentNodeInfo.getPackageName().toString();
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            windowInfo = parentNodeInfo.getWindow();
//        }
//
//        Intent intent = new Intent(ACCESSIBILITY_INTENT);
//        //Gets the package name of the source
//        intent.putExtra(ACCESSIBILITY_NAME, packageName);
//        //Gets the event type
//        intent.putExtra(ACCESSIBILITY_EVENT_TYPE, eventType);
//        //Gets the performed action that triggered this event.
//        intent.putExtra(ACCESSIBILITY_ACTION, accessibilityEvent.getAction());
//        //Gets The event time.
//        intent.putExtra(ACCESSIBILITY_EVENT_TIME, accessibilityEvent.getEventTime());
//        //Gets the movement granularity that was traversed.
//        intent.putExtra(ACCESSIBILITY_MOVEMENT, accessibilityEvent.getMovementGranularity());
//
//        // Gets the node bounds in screen coordinates.
//        Rect rect = new Rect();
//        parentNodeInfo.getBoundsInScreen(rect);
//        intent.putExtra(ACCESSIBILITY_SCREEN_BOUNDS, getBoundingPoints(rect));
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            //Gets the bit mask of change types signaled by a TYPE_WINDOW_CONTENT_CHANGED event or TYPE_WINDOW_STATE_CHANGED. A single event may represent multiple change types.
//            intent.putExtra(ACCESSIBILITY_CHANGES_TYPES, accessibilityEvent.getContentChangeTypes());
//        }
//        if (parentNodeInfo.getText() != null) {
//            //Gets the text of this node.
//            intent.putExtra(ACCESSIBILITY_TEXT, parentNodeInfo.getText().toString());
//        }
//        getNextTexts(parentNodeInfo, nextTexts);
//
//        //Gets the text of sub nodes.
//        intent.putStringArrayListExtra(ACCESSIBILITY_NODES_TEXT, (ArrayList<String>) nextTexts);
//
//        if (windowInfo != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                // Gets if this window is active.
//                intent.putExtra(ACCESSIBILITY_IS_ACTIVE, windowInfo.isActive());
//                // Gets if this window has input focus.
//                intent.putExtra(ACCESSIBILITY_IS_FOCUSED, windowInfo.isFocused());
//                // Gets the type of the window.
//                intent.putExtra(ACCESSIBILITY_WINDOW_TYPE, windowInfo.getType());
//                // Check if the window is in picture-in-picture mode.
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    intent.putExtra(ACCESSIBILITY_IS_PIP, windowInfo.isInPictureInPictureMode());
//                }
//
//            }
//        }
//        sendBroadcast(intent);
    }
    void getNextTexts(AccessibilityNodeInfo node, List<String> arr) {
        if (node.getText() != null && node.getText().length() > 0)
            arr.add(node.getText().toString());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null)
                continue;
            getNextTexts(child, arr);
        }

    }
    private HashMap<String, Integer> getBoundingPoints(Rect rect) {
        HashMap<String, Integer> frame = new HashMap<>();
        frame.put("left", rect.left);
        frame.put("right", rect.right);
        frame.put("top", rect.top);
        frame.put("bottom", rect.bottom);
        frame.put("width", rect.width());
        frame.put("height", rect.height());
        return frame;
    }
    @Override
    public void onInterrupt() {

    }
}

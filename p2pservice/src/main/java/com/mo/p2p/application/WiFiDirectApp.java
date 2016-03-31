package com.mo.p2p.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Message;
import android.util.Log;

import com.colorcloud.wifichat.AppPreferences;
import com.colorcloud.wifichat.ConnectionService;
import com.colorcloud.wifichat.JSONUtils;
import com.colorcloud.wifichat.MessageRow;
import com.colorcloud.wifichat.WiFiDirectActivity;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.mo.p2p.activity.Search_activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.colorcloud.wifichat.Constants.MSG_STARTCLIENT;
import static com.colorcloud.wifichat.Constants.MSG_STARTSERVER;

public class WiFiDirectApp extends Application {

    private static final String TAG = "PTP_APP";

    public WifiP2pManager mP2pMan = null;
    ;
    public Channel mP2pChannel = null;
    public boolean mP2pConnected = false;
    public String mMyAddr = null;
    public String mDeviceName = null;   // the p2p name that is configurated from UI.

    public WifiP2pDevice mThisDevice = null;
    public WifiP2pInfo mP2pInfo = null;  // set when connection info available, reset when WIFI_P2P_CONNECTION_CHANGED_ACTION

    public boolean mIsServer = false;

    public WiFiDirectActivity mHomeActivity = null;
    public Search_activity mSearchActivity = null;
    public List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();  // update on every peers available
    public JSONArray mMessageArray = new JSONArray();        // limit to the latest 50 messages

    @Override
    public void onCreate() {
        super.onCreate();
        initIM();
    }

    /**
     * 初始化环信
     */
    private void initIM() {

//        EMChat.getInstance().init(this);
//        EMChat.getInstance().setDebugMode(true);//在做打包混淆时，要关闭debug模式，避免消耗不必要的资源
        EMOptions options = new EMOptions();
        // 默认添加好友时，是不需要验证的，改成需要验证
        options.setAcceptInvitationAlways(false);
//        Context appContext = this;
//        int pid = android.os.Process.myPid();
//        String processAppName = getAppName(pid);
//        // 如果app启用了远程的service，此application:onCreate会被调用2次
//        // 为了防止环信SDK被初始化2次，加此判断会保证SDK被初始化1次
//        // 默认的app会在以包名为默认的process name下运行，如果查到的process name不是app的process name就立即返回
//
//        if (processAppName == null || !processAppName.equalsIgnoreCase(appContext.getPackageName())) {
//            Log.e(TAG, "enter the service process!");
//            //"com.easemob.chatuidemo"为demo的包名，换到自己项目中要改成自己包名
//
//            // 则此application::onCreate 是被service 调用的，直接返回
//            return;
//        }
        //初始化

        EMClient.getInstance().init(this, options);
//        在做打包混淆时，关闭debug模式，避免消耗不必要的资源
        EMClient.getInstance().setDebugMode(true);

    }

    private String getAppName(int pID) {
        String processName = null;
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        PackageManager pm = this.getPackageManager();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pID) {
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                // Log.d("Process", "Error>> :"+ e.toString());
            }
        }
        return processName;
    }

    /**
     * whether p2p is enabled in this device.
     * my bcast listener always gets enable/disable intent and persist to shared pref
     */
    public boolean isP2pEnabled() {
        String state = AppPreferences.getStringFromPref(this, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED);
        if (state != null && "1".equals(state.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 开启Socket服务器端
     */
    public void startSocketServer() {
        Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
        msg.what = MSG_STARTSERVER;
        ConnectionService.getInstance().getHandler().sendMessage(msg);
    }

    /**
     * 开启客户端
     */
    public void startSocketClient(String hostname) {
        Log.d(TAG, "startSocketClient : client connect to group owner : " + hostname);
        Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
        msg.what = MSG_STARTCLIENT;
        msg.obj = hostname;
        ConnectionService.getInstance().getHandler().sendMessage(msg);
    }

    /**
     * check whether there exists a connected peer.
     */
    public WifiP2pDevice getConnectedPeer() {
        WifiP2pDevice peer = null;
        for (WifiP2pDevice d : mPeers) {
            PTPLog.d(TAG, "getConnectedPeer : device : " + d.deviceName + " status: " + ConnectionService.getDeviceStatus(d.status));
            if (d.status == WifiP2pDevice.CONNECTED) {
                peer = d;
            }
        }
        return peer;
    }

    /**
     * insert a json string msg into messages json array
     */
    public void shiftInsertMessage(String jsonmsg) {
        JSONObject jsonobj = JSONUtils.getJsonObject(jsonmsg);
        mMessageArray.put(jsonobj);
        mMessageArray = JSONUtils.truncateJSONArray(mMessageArray, 10);  // truncate the oldest 10.
    }

    public String shiftInsertMessage(MessageRow row) {
        JSONObject jsonobj = MessageRow.getAsJSONObject(row);
        if (jsonobj != null) {
            mMessageArray.put(jsonobj);
        }
        mMessageArray = JSONUtils.truncateJSONArray(mMessageArray, 10);  // truncate the oldest 10.
        return jsonobj.toString();
    }

    public void clearMessages() {
        mMessageArray = new JSONArray();
    }

    /**
     * get the intent to lauch any activity
     */
    public Intent getLauchActivityIntent(Class<?> cls, String initmsg) {
        Intent i = new Intent(this, cls);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("FIRST_MSG", initmsg);
        return i;
    }

    public void setMyAddr(String addr) {
        mMyAddr = addr;
    }

    public static class PTPLog {
        public static void i(String tag, String msg) {
            Log.i(tag, msg);
        }

        public static void d(String tag, String msg) {
            Log.d(tag, msg);
        }

        public static void e(String tag, String msg) {
            Log.e(tag, msg);
        }
    }

}

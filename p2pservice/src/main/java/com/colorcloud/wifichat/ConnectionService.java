package com.colorcloud.wifichat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mo.p2p.tool.L;

import java.nio.channels.SocketChannel;

import static com.colorcloud.wifichat.Constants.MSG_BROKEN_CONN;
import static com.colorcloud.wifichat.Constants.MSG_FINISH_CONNECT;
import static com.colorcloud.wifichat.Constants.MSG_NEW_CLIENT;
import static com.colorcloud.wifichat.Constants.MSG_NULL;
import static com.colorcloud.wifichat.Constants.MSG_PULLIN_DATA;
import static com.colorcloud.wifichat.Constants.MSG_PUSHOUT_DATA;
import static com.colorcloud.wifichat.Constants.MSG_REGISTER_ACTIVITY;
import static com.colorcloud.wifichat.Constants.MSG_SELECT_ERROR;
import static com.colorcloud.wifichat.Constants.MSG_STARTCLIENT;
import static com.colorcloud.wifichat.Constants.MSG_STARTSERVER;

public class ConnectionService extends Service implements ChannelListener, PeerListListener, ConnectionInfoListener {  // callback of requestPeers{

    private static final String TAG = "PTP_Serv";

    private static ConnectionService _sinstance = null;

    private WorkHandler mWorkHandler;
    private MessageHandler mHandler;

    boolean retryChannel = false;

    WiFiDirectApp mApp;
    MainActivity mActivity;    // shall I use weak reference here ?
    ConnectionManager mConnMan;

    /**
     * @see Service#onCreate()
     */
    private void _initialize() {
        if (_sinstance != null) {
            L.d(TAG, "_initialize, already initialized, do nothing.");
            return;
        }

        _sinstance = this;
        mWorkHandler = new WorkHandler(TAG);
        mHandler = new MessageHandler(mWorkHandler.getLooper());

        mApp = (WiFiDirectApp) getApplication();
        mApp.mP2pMan = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
        L.d(TAG, "_initialize, get p2p service and init channel !!!");

        mConnMan = new ConnectionManager(this);
    }

    public static ConnectionService getInstance() {
        return _sinstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _initialize();
        processIntent(intent);
        return START_STICKY;
    }

    /**
     * 处理所有wifi p2p意图被广播地址recv。
     * P2P连接设置事件序列:
     * 1。找到后,peers_changed可用,邀请
     * 2。当连接建立,这个设备连接。
     * 3。对于服务器来说,WIFI_P2P_CONNECTION_CHANGED_ACTION意图:p2p连接,
     * 为客户端,这个设备更改为连接第一,然后CONNECTION_CHANGED
     * 4。WIFI_P2P_PEERS_CHANGED_ACTION:对等连接。
     * 5。现在这个设备和同行都是连接!
     * <p/>
     * 如果选择p2p服务器模式和创建群,这个设备将自动组所有者
     * 1。这个设备连接
     * 2。WIFI_P2P_CONNECTION_CHANGED_ACTION
     */
    private void processIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        L.d(TAG, "processIntent: " + intent.toString());

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {  // this devices's wifi direct enabled state.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
                AppPreferences.setStringToPref(mApp, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED, "1");
                L.d(TAG, "processIntent : WIFI_P2P_STATE_CHANGED_ACTION : enabled, re-init p2p channel to framework ");
            } else {
                mApp.mThisDevice = null;    // reset this device status
                mApp.mP2pChannel = null;
                mApp.mPeers.clear();
                L.d(TAG, "processIntent : WIFI_P2P_STATE_CHANGED_ACTION : disabled, null p2p channel to framework ");
                if (mApp.mHomeActivity != null) {
                    mApp.mHomeActivity.updateThisDevice(null);
                    mApp.mHomeActivity.resetData();
                }
                if (mApp.mSearchActivity != null) {
                    mApp.mSearchActivity.updateThisDevice(null);
                    mApp.mSearchActivity.clearPeers();
                }
                AppPreferences.setStringToPref(mApp, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED, "0");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {    //peers列表
            if (mApp.mP2pMan != null) {
                mApp.mP2pMan.requestPeers(mApp.mP2pChannel, this);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {  //连接发生改变时
            if (mApp.mP2pMan == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            L.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION : " + networkInfo.getReason() + " : " + networkInfo.toString());
            if (networkInfo.isConnected()) {
                Log.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION: p2p connected ");
                // 与其他设备连接,请求连接组所有者IP信息。调内部细节片段。
                mApp.mP2pMan.requestConnectionInfo(mApp.mP2pChannel, this);

            } else {
                L.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION: p2p disconnected, mP2pConnected = false..closeClient.."); // It's a disconnect
                mApp.mP2pConnected = false;
                mApp.mP2pInfo = null;   // reset connection info after connection done.
                mConnMan.closeClient();

                if (mApp.mHomeActivity != null) {
                    mApp.mHomeActivity.resetData();
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // this device details has changed(name, connected, etc)
            mApp.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mApp.mDeviceName = mApp.mThisDevice.deviceName;
            L.d(TAG, "processIntent: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION " + mApp.mThisDevice.deviceName);
            if (mApp.mHomeActivity != null) {
                mApp.mHomeActivity.updateThisDevice(mApp.mThisDevice);
            }

            if (mApp.mSearchActivity != null) {
                mApp.mSearchActivity.updateThisDevice(null);
            }

            if (mApp.mSearchActivity != null) {
                mApp.mSearchActivity.updateThisDevice(mApp.mThisDevice);
            }
        }
    }

    /**
     * 通道框架Wifi P2p已断开连接。可以尝试重新初始化
     */
    @Override
    public void onChannelDisconnected() {
        if (!retryChannel) {
            L.d(TAG, "onChannelDisconnected : retry initialize() ");
            mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
            if (mApp.mHomeActivity != null) {
                mApp.mHomeActivity.resetData();
            }
            retryChannel = true;
        } else {
            L.d(TAG, "onChannelDisconnected : stop self, ask user to re-enable.");
            if (mApp.mHomeActivity != null) {
                mApp.mHomeActivity.onChannelDisconnected();
            }
            stopSelf();
        }
    }

    /**
     * 回调搜索到的设备
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        mApp.mPeers.clear();
        mApp.mPeers.addAll(peerList.getDeviceList());

        WifiP2pDevice connectedPeer = mApp.getConnectedPeer();   //获取正在连接的Device对象
        if (connectedPeer != null) {
            L.d(TAG, "onPeersAvailable : exist connected peer : " + connectedPeer.deviceName);
        } else {

        }

        if (mApp.mP2pInfo != null && connectedPeer != null) {
            if (mApp.mP2pInfo.groupFormed && mApp.mP2pInfo.isGroupOwner) {
                L.d(TAG, "onPeersAvailable : 设备是管理者: 开启服务端socket");

                mApp.startSocketServer();
            } else if (mApp.mP2pInfo.groupFormed && connectedPeer != null) {
                // XXX 客户端路径信息之后可用连接建立连接。
                L.d(TAG, "onConnectionInfoAvailable: 设备是客户端,连接到组所有者 ");
                mApp.startSocketClient(mApp.mP2pInfo.groupOwnerAddress.getHostAddress());
            }
        }
        if (mApp.mSearchActivity != null) {
            mApp.mSearchActivity.updateDeviceList(peerList);
        }
    }

    /**
     * 回调时_Requested_ connectino信息是可用的。
     * WIFI_P2P_CONNECTION_CHANGED_ACTION意图,requestConnectionInfo()
     */
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable: " + info.groupOwnerAddress.getHostAddress());
        if (info.groupFormed && info.isGroupOwner) {   //开启服务端
            // XXX server path goes to peer connected.
//            new DeviceDetailFragment.FileServerAsyncTask(getApplication(), mContentView.findViewById(R.id.status_text)).execute();
            L.d(TAG, "onConnectionInfoAvailable: device is groupOwner: startSocketServer ");
            mApp.startSocketServer();
        } else if (info.groupFormed) {   //开启客户端
            L.d(TAG, "onConnectionInfoAvailable: device is client, connect to group owner: startSocketClient ");
            mApp.startSocketClient(info.groupOwnerAddress.getHostAddress());
        }
        mApp.mP2pConnected = true;
        mApp.mP2pInfo = info;   // connection info available
    }

    private void enableStartChatActivity() {
        if (mApp.mHomeActivity != null) {
            L.d(TAG, "enableStartChatActivity :  nio channel ready, enable start chat !");
            mApp.mHomeActivity.onConnectionInfoAvailable(mApp.mP2pInfo);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * 消息处理程序轮询处理所有的msg送到位置管理器。
     */
    final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processMessage(msg);
        }
    }

    /**
     * 主要信息处理过程轮询。
     */
    private void processMessage(Message msg) {
        switch (msg.what) {
            case MSG_NULL:
                break;
            case MSG_REGISTER_ACTIVITY:
                L.d(TAG, "processMessage: onActivityRegister to chat fragment...");
                onActivityRegister((MainActivity) msg.obj, msg.arg1);
                break;
            case MSG_STARTSERVER:
                L.d(TAG, "processMessage: startServerSelector...");
                if (mConnMan.startServerSelector() >= 0) {
                    enableStartChatActivity();
                }
                break;
            case MSG_STARTCLIENT:
                L.d(TAG, "processMessage: startClientSelector...");
                if (mConnMan.startClientSelector((String) msg.obj) >= 0) {
                    enableStartChatActivity();
                }
                break;
            case MSG_NEW_CLIENT:
                L.d(TAG, "processMessage:  onNewClient...");
                mConnMan.onNewClient((SocketChannel) msg.obj);
                break;
            case MSG_FINISH_CONNECT:
                L.d(TAG, "processMessage:  onFinishConnect...");
                mConnMan.onFinishConnect((SocketChannel) msg.obj);
                break;
            case MSG_PULLIN_DATA:                 //接收数据
                L.d(TAG, "processMessage:  onPullIndata ...");
                onPullInData((SocketChannel) msg.obj, msg.getData());
                break;
            case MSG_PUSHOUT_DATA:               //发送数据
                L.d(TAG, "processMessage: onPushOutData...");
                onPushOutData((String) msg.obj);
                break;
            case MSG_SELECT_ERROR:
                L.d(TAG, "processMessage: onSelectorError...");
                mConnMan.onSelectorError();
                break;
            case MSG_BROKEN_CONN:
                L.d(TAG, "processMessage: onBrokenConn...");
                mConnMan.onBrokenConn((SocketChannel) msg.obj);
                break;
            default:
                break;
        }
    }

    /**
     * 注册使用该服务的活动。
     */
    private void onActivityRegister(MainActivity activity, int register) {
        Log.d(TAG, "onActivityRegister : activity register itself to service : " + register);
        if (register == 1) {
            mActivity = activity;
        } else {
            mActivity = null;    // set to null explicitly to avoid mem leak.
        }
    }

    /**
     * 服务处理数据来自套接字通道，  ————————>  接受数据
     */
    private String onPullInData(SocketChannel schannel, Bundle b) {
        String data = b.getString("DATA");
        Log.d(TAG, "onDataIn : recvd msg : " + data);
        mConnMan.onDataIn(schannel, data);  // pub to all client if this device is server.
        MessageRow row = MessageRow.parseMessageRow(data);
        // now first add to app json array
        mApp.shiftInsertMessage(row);
        showNotification(row);
        // add to activity if it is on focus.
        showInActivity(row);
        return data;
    }

    /**
     * 发送数据
     * <p/>
     * 处理数据推送请求。
     * 如果发送方服务器,发送到所有客户端。
     * 如果发送方客户,只能发送到服务器。
     */
    private void onPushOutData(String data) {
        Log.d(TAG, "onPushOutData : " + data);
        mConnMan.pushOutData(data);
    }

    /**
     * 同步调用发送数据使用康涅狄格州男子的频道,康涅狄格州的人现在选择上阻塞
     */
    public int connectionSendData(String jsonstring) {
        Log.d(TAG, "connectionSendData : " + jsonstring);
        new SendDataAsyncTask(mConnMan, jsonstring).execute();
        return 0;
    }

    /**
     * 为了避免NetworkOnMainThreadException写入数据的异步任务。
     */
    public class SendDataAsyncTask extends AsyncTask<Void, Void, Integer> {
        private String data;
        private ConnectionManager connman;

        public SendDataAsyncTask(ConnectionManager conn, String jsonstring) {
            connman = conn;
            data = jsonstring;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return connman.pushOutData(data);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "SendDataAsyncTask : onPostExecute:  " + data + " len: " + result);
        }
    }

    /**
     * 发送一个通知recv数据后,单击通知将广播地址悬而未决的意图,这
     * 将推出chatactivity片段。
     */
    public void showNotification(MessageRow row) {

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//    	Notification notification = new Notification(R.drawable.ic_action_discover, row.mMsg, System.currentTimeMillis());
//    	notification.defaults |= Notification.DEFAULT_VIBRATE;
//    	CharSequence title = row.mSender;
//    	CharSequence text = row.mMsg;
        //Intent notificationIntent = new Intent(this, WiFiDirectActivity.class);
        Intent notificationIntent = mApp.getLauchActivityIntent(MainActivity.class, row.mMsg);
        // pendingIntent that will start a new activity.
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
//    	notification.setLatestEventInfo(this, title, text, contentIntent);

//    	notificationManager.notify(1, notification);
        L.d(TAG, "showNotification: " + row.mMsg);

        Notification notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle(row.mSender)
                .setContentText(row.mMsg)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_action_discover)
                .setWhen(System.currentTimeMillis())
                .build();
        notificationManager.notify(1, notification);
    }

    /**
     * 显示活动的消息
     */
    private void showInActivity(final MessageRow row) {
        L.d(TAG, "showInActivity : " + row.mMsg);
        if (mActivity != null) {
            mActivity.showMessage(row);
        } else {
            if (mApp.mHomeActivity != null && mApp.mHomeActivity.mHasFocus == true) {
                L.d(TAG, "showInActivity :  chat activity down, force start only when home activity has focus !");
                mApp.mHomeActivity.startChatActivity(row.mMsg);
            } else {
                L.d(TAG, "showInActivity :  Home activity down, do nothing, notification will launch it...");
            }
        }
    }

    public static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "可用";
            case WifiP2pDevice.INVITED:
                return "邀请";
            case WifiP2pDevice.CONNECTED:
                return "连接";
            case WifiP2pDevice.FAILED:
                return "失败";
            case WifiP2pDevice.UNAVAILABLE:
                return "不可用";
            default:
                return "未知的 = " + deviceStatus;
        }
    }
}

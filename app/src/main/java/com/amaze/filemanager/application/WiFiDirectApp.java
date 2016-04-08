package com.amaze.filemanager.application;

import android.app.Application;

import com.easemob.chat.EMChat;

public class WiFiDirectApp extends Application {

    private static final String TAG = "PTP_APP";

    @Override
    public void onCreate() {
        super.onCreate();
        initIM();
    }

    /**
     * 初始化环信
     */
    private void initIM() {
        EMChat.getInstance().setAppkey("momxmo#p2p");
        EMChat.getInstance().init(this);
        EMChat.getInstance().setDebugMode(true);//在做打包混淆时，要关闭debug模式，避免消耗不必要的资源
    }
}

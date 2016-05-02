package com.easemob.easeui;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.easeui.controller.EaseUI;
import com.easemob.exceptions.EaseMobException;

public class DemoApplication extends Application {
    private static final String TAG = "DemoApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        EMChat.getInstance().setAppkey("momxmo#p2p");
        EaseUI.getInstance().init(this);
        initFirend();
    }

    private void initFirend() {
        //注册一个好友请求等的BroadcastReceiver
//        IntentFilter inviteIntentFilter = new IntentFilter(EMChatManager.getInstance().getContactInviteEventBroadcastAction());
        Log.d(TAG,  "主句好友请求监听" );
//        registerReceiver(contactInviteReceiver, inviteIntentFilter);


    }

    private BroadcastReceiver contactInviteReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //请求理由
            final String reason = intent.getStringExtra("reason");
            final boolean isResponse = intent.getBooleanExtra("isResponse", false);
            //消息发送方username
            final String from = intent.getStringExtra("username");
            //sdk暂时只提供同意好友请求方法，不同意选项可以参考微信增加一个忽略按钮。
            if (!isResponse) {
                Log.d(TAG, from + "请求加你为好友,reason: " + reason);
                //同意username的好友请求
                try {
                    EMChatManager.getInstance().acceptInvitation(from);//需异步处理
                } catch (EaseMobException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, from + "同意了你的好友请求");
            }
            //具体ui上的处理参考chatuidemo。
        }
    };
}

package com.mo.p2p.event;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.View;

import com.mo.p2p.activity.Search_activity;

/**
 * Created by MomxMo on 2016/3/29.
 *
 * 点击连接事件
 */
public class ConnectDeviceListener implements View.OnClickListener {
    WifiP2pDevice device;
    Search_activity search_activity;
    public ConnectDeviceListener(WifiP2pDevice device, Search_activity search_activity) {
        this.device = device;
        this.search_activity = search_activity;
    }

    @Override
    public void onClick(View v) {
        search_activity.stopSearch();  //停止动画
    }
}

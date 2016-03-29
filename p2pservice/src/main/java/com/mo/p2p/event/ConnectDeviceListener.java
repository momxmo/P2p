package com.mo.p2p.event;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
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
    ProgressDialog progressDialog = null;
    public ConnectDeviceListener(WifiP2pDevice device, Search_activity search_activity) {
        this.device = device;
        this.search_activity = search_activity;
    }

    @Override
    public void onClick(View v) {
        search_activity.stopSearch();  //停止动画

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;  // least inclination to be group owner.
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(search_activity, "返回键取消",
                "连接:" + device.deviceAddress, true, true,  // cancellable
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {   //取消连接
                        search_activity.cancelDisconnect();
                    }
                });
        search_activity.connect(config);
    }
}

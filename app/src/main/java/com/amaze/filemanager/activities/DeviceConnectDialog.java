package com.amaze.filemanager.activities;

import android.app.Dialog;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.DeviceChooseFragment;
import com.amaze.filemanager.p2p.WifiP2pHelper;

import java.util.ArrayList;


public class DeviceConnectDialog extends Dialog {

    private MainActivity activity;
    private DeviceChooseFragment fragment_choose_device;
    /*
     * public PlayerDialog(Context context) { super(context); // TODO
     * Auto-generated constructor stub }
     */
    public DeviceConnectDialog(MainActivity activity, int style) {
        super(activity, style);
        this.activity = activity;
    }

    public DeviceConnectDialog(MainActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public void show() {
        super.show();
        fragment_choose_device.init();
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        // set contentView:

        View v = getLayoutInflater().inflate(R.layout.dialog_connect_device, null);
        setContentView(v);
        fragment_choose_device = (DeviceChooseFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_choose_device);
        Log.d(WifiP2pHelper.TAG, "--->"+fragment_choose_device);
        // set the dialog inflate the whole screen
        Display dis = activity.getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        // lp.height=dis.getHeight();
        lp.width = dis.getWidth();
        lp.height = activity.getWindow().getAttributes().height;
        getWindow().setAttributes(lp);
    }

    //update views-->由Activity调用
    public void updateDeviceList(ArrayList<WifiP2pDevice> deviceList) {
        if(this.fragment_choose_device != null) {
            this.fragment_choose_device.updateDeviceList(deviceList);
        }
    }
    public void updateConnectedInfo(boolean isServer) {
        if(this.fragment_choose_device != null) {
            this.fragment_choose_device.updateConnectedInfo(isServer);
        }
    }
    public void onDisconnectedInfo() {
        if(this.fragment_choose_device != null) {
            this.fragment_choose_device.onDisconnectedInfo();
        }
    }
}

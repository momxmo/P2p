package mo.com.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MomxMo on 2016/4/11.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener{

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity  mActivity;
    private TextView  con_device;
    protected List<WifiP2pDevice> deviceList = new ArrayList<WifiP2pDevice>();

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity  activity,TextView con_device) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.con_device = con_device;

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.i("p2p", "Wifi P2P is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                Log.i("p2p", "Wi-Fi P2P is not enabled");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.i("p2p", "1");
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if (mManager != null) {
                mManager.requestPeers(mChannel, this);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            Log.i("p2p", "2");
            if (deviceList.size() > 0) {
                connect(deviceList.get(0));
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            Log.i("p2p", "3");
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.i("p2p", "设备列表数目:" + peers.getDeviceList().size());
        deviceList.clear();
        deviceList.addAll(peers.getDeviceList());
        for (WifiP2pDevice device : deviceList) {
            Log.i("p2p", "设备名称:" + device.deviceName + "  地址：" + device.deviceAddress);

        }
    }

    public void connect(final WifiP2pDevice device) {
        // Picking the first device found on the network.
//        WifiP2pDevice device = peers.get(0);

        MainActivity.address = device.deviceAddress;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                con_device.setText("连接成功" + device.deviceAddress);
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Toast.makeText(mActivity, "连接成功" + device.deviceAddress,
                        Toast.LENGTH_SHORT).show();
                Log.i("p2p", "连接成功:" + device.deviceAddress + " name:" + device.deviceName);
            }

            @Override
            public void onFailure(int reason) {
                con_device.setText("连接失败");
                Toast.makeText(mActivity, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}






















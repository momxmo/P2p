package com.mo.p2p.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.colorcloud.wifichat.DeviceListFragment;
import com.colorcloud.wifichat.R;
import com.colorcloud.wifichat.WiFiDirectApp;
import com.mo.p2p.event.ConnectDeviceListener;
import com.mo.p2p.tool.Dimens;
import com.mo.p2p.tool.L;
import com.mo.p2p.tool.T;
import com.mo.p2p.view.RippleBackground;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 开启搜索功能
 */

public class Search_activity extends Activity implements View.OnClickListener, DeviceListFragment.DeviceActionListener {

    private static final java.lang.String TAG = "Search_activity";
    private ImageView foundDevice;
    RippleBackground rippleBackground;
    ImageView mStartSearch;
    TextView mTvThisDevice;
    WiFiDirectApp mApp = null;
    List<View> deviceViewList = new ArrayList<>();
    List<Integer> ranList = new ArrayList<>();
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);
        mApp = (WiFiDirectApp) getApplication();
        mApp.mSearchActivity = this;
        initView();
        initEvent();
    }

    private void initEvent() {
        mStartSearch.setOnClickListener(this);
    }

    private void initView() {
        rippleBackground = (RippleBackground) findViewById(R.id.content);
        foundDevice = (ImageView) findViewById(R.id.foundDevice);
        mStartSearch = (ImageView) findViewById(R.id.centerImage);
        mTvThisDevice = (TextView) findViewById(R.id.tv_myDevice);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mApp.mThisDevice != null) {
            WiFiDirectApp.PTPLog.d(TAG, "onResume : redraw this device details");
            updateThisDevice(mApp.mThisDevice);

            // 果p2p性连接信息可用,我状态连接,启用开始聊天!
            if (mApp.mP2pInfo != null && mApp.mThisDevice.status == WifiP2pDevice.CONNECTED) {
                WiFiDirectApp.PTPLog.d(TAG, "onResume : redraw detail fragment");
//                onConnectionInfoAvailable(mApp.mP2pInfo);
            } else {
                // XXX stop client, if any.
            }
        }
    }

    /**
     * 停止搜索,     停止动画
     */
    public void stopSearch() {
        rippleBackground.stopRippleAnimation();
    }


    public void clearPeers() {
        for (View view : deviceViewList) {
            rippleBackground.removeView(view);
        }
        deviceViewList.clear();
        ranList.clear();
    }

    /**
     * 更新数据
     */
    public void updateDeviceList(WifiP2pDeviceList deviceList) {
        clearPeers();

        List<WifiP2pDevice> peersList = new ArrayList<>();
        peersList.addAll(deviceList.getDeviceList());
        for (WifiP2pDevice device : peersList) {
            add_device(R.drawable.phone2, device, 1);
        }
    }

    /**
     * process WIFI_P2P_THIS_DEVICE_CHANGED_ACTION intent, refresh this device.
     */
    public void updateThisDevice(final WifiP2pDevice device) {
        if (device != null) {
            mTvThisDevice.setText(device.deviceName);
        }
    }


    /**
     * 添加显示
     */
    public View add_device(int headResource, WifiP2pDevice device, int pos) {

        View mDeviceView = View.inflate(this, R.layout.item_device, null);
        mDeviceView.setOnClickListener(new ConnectDeviceListener(device, this));
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (pos == 1) {  //左上角
            params.addRule(RelativeLayout.LEFT_OF, R.id.centerImage);
            params.setMargins(0, (int) Dimens.dpToPx(120), (int) Dimens.dpToPx(6), (int) Dimens.dpToPx(32));
        } else if (pos == 2) {   //左边中角
            params.addRule(RelativeLayout.LEFT_OF, R.id.centerImage);
            params.setMargins(0, (int) Dimens.dpToPx(220), (int) Dimens.dpToPx(15), 0);
        } else if (pos == 3) { //左下角
            params.addRule(RelativeLayout.BELOW, R.id.centerImage);
            params.setMargins((int) Dimens.dpToPx(40), (int) Dimens.dpToPx(20), 0, 0);
        } else if (pos == 4) { //正下方
            params.addRule(RelativeLayout.BELOW, R.id.centerImage);
            params.setMargins((int) Dimens.dpToPx(140), (int) Dimens.dpToPx(10), 0, 0);
        } else if (pos == 5) { //右下角
            params.addRule(RelativeLayout.BELOW, R.id.centerImage);
            params.setMargins((int) Dimens.dpToPx(210), (int) Dimens.dpToPx(18), 0, 0);
        } else if (pos == 6) { //右正方
            params.addRule(RelativeLayout.RIGHT_OF, R.id.centerImage);
            params.setMargins((int) Dimens.dpToPx(15), (int) Dimens.dpToPx(180), 0, 0);
        }
        mDeviceView.setLayoutParams(params);
        mDeviceView.setVisibility(View.GONE);
        rippleBackground.addView(mDeviceView);
        foundDevice(mDeviceView);

        ImageView mIVfonudDevice = (ImageView) mDeviceView.findViewById(R.id.foundDevice);
        TextView mTVdeviceName = (TextView) mDeviceView.findViewById(R.id.tv_device_name);
        if (headResource != -1) {
            mIVfonudDevice.setBackgroundResource(headResource);
        }
        if (device != null) {
            mTVdeviceName.setText(device.deviceName);
        }
        return mDeviceView;
    }

    /**
     * 随机选着位置
     *
     * @return
     */
    public int getRandom(int range) {
        Random random = new Random();
        int ranInt = random.nextInt(range)+1;
        while (ranList.contains(ranInt)) {
            if (ranInt < 0 || ranInt > range) {
                ranInt = 0;
            } else {
                ranInt++;
            }
        }
        return ranInt;
    }

    /**
     * 动画展示搜素到的设备
     *
     * @param view
     */
    private void foundDevice(View view) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        ArrayList<Animator> animatorList = new ArrayList<Animator>();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f, 1.6f, 0.8f);
        animatorList.add(scaleXAnimator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f, 1.6f, 0.8f);
        animatorList.add(scaleYAnimator);
        animatorSet.playTogether(animatorList);
        view.setVisibility(View.VISIBLE);
        animatorSet.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.centerImage:  //开始搜索
                startSearch();
                break;
        }
    }

    /**
     * 开始搜索
     */
    public void startSearch() {
        rippleBackground.startRippleAnimation();
        final int range = 6;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 6; i++) {
                    final WifiP2pDevice device = new WifiP2pDevice();
                    device.deviceName = i + "号设备";
                    View deviceView = add_device(R.drawable.phone2, device, getRandom(range));
                    deviceViewList.add(deviceView);

                }

            }
        }, 3000);
    }

    /**
     * 展示设备信息
     *
     * @param device
     */
    @Override
    public void showDetails(WifiP2pDevice device) {

    }

    /**
     * 取消连接
     */
    @Override
    public void cancelDisconnect() {
        if (mApp.mP2pMan != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);

            if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE || fragment.getDevice().status == WifiP2pDevice.INVITED) {
                mApp.mP2pMan.cancelConnect(mApp.mP2pChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        T.showToast(Search_activity.this, "Aborting connection ", Toast.LENGTH_SHORT);
                        L.d(TAG, "cancelConnect : success canceled...");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        T.showToast(Search_activity.this, "cancelConnect: request failed. Please try again..", Toast.LENGTH_SHORT);
                        L.d(TAG, "cancelConnect : cancel connect request failed..." + reasonCode);
                    }
                });
            }
        }
    }

    /**
     * 建立连接
     *
     * @param config
     */
    @Override
    public void connect(WifiP2pConfig config) {
        L.d(TAG, "connect : connect to server : " + config.deviceAddress);
        // perform p2p connect upon users click the connect button. after connection, manager request connection info.

        mApp.mP2pMan.connect(mApp.mP2pChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                T.showToast(Search_activity.this, "Connect success..", Toast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(int reason) {
                T.showToast(Search_activity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT);
            }
        });
    }


    /**
     * 断开连接
     */
    @Override
    public void disconnect() {
        mApp.mP2pMan.removeGroup(mApp.mP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                L.d(TAG, "Disconnect failed. Reason : 1=error, 2=busy; " + reasonCode);
                T.showToast(Search_activity.this, "disconnect failed.." + reasonCode, Toast.LENGTH_SHORT);
            }

            @Override
            public void onSuccess() {
                L.d(TAG, "Disconnect succeed. ");
            }
        });
    }
}

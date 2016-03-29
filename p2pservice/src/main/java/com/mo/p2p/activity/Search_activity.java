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
        mApp = (WiFiDirectApp)getApplication();
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
    }
    /**
     * 更新数据
     */
    public void updateDeviceList(WifiP2pDeviceList deviceList) {
        clearPeers();

        List<WifiP2pDevice> peersList = new ArrayList<>();
        peersList.addAll(deviceList.getDeviceList());
        for (WifiP2pDevice device : peersList) {
            add_device(R.drawable.phone2, device);
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
    public View add_device(int headResource, WifiP2pDevice device) {
        View mDeviceView = View.inflate(this, R.layout.item_device, null);
        mDeviceView.setOnClickListener(new ConnectDeviceListener(device, this));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, R.id.centerImage);



        params.setMargins(0, (int) Dimens.dpToPx(120), (int) Dimens.dpToPx(6), (int) Dimens.dpToPx(32));
        mDeviceView.setLayoutParams(params);
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

    public int getRandom() {
        Random random = new Random();
        int ranInt = random.nextInt(4);
        boolean contains = ranList.contains(ranInt);
        if (contains) {
            getRandom();
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
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        ArrayList<Animator> animatorList = new ArrayList<Animator>();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f);
        animatorList.add(scaleXAnimator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f);
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
        final WifiP2pDevice device = new WifiP2pDevice();
        device.deviceName = "一号设备";

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                View deviceView = add_device(R.drawable.phone2, device);
                deviceViewList.add(deviceView);
            }
        }, 3000);
    }

    /**
     * 展示设备信息
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

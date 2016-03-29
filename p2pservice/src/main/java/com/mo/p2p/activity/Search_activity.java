package com.mo.p2p.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.colorcloud.wifichat.R;
import com.mo.p2p.event.ConnectDeviceListener;
import com.mo.p2p.tool.Dimens;
import com.mo.p2p.view.RippleBackground;

import java.util.ArrayList;
import java.util.List;

/**
 * 开启搜索功能
 */

public class Search_activity extends Activity implements View.OnClickListener{

    private ImageView foundDevice;
    RippleBackground rippleBackground;
    ImageView mStartSearch;
    List<View> deviceViewList = new ArrayList<>();
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);
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
    }


    /**
     * 停止搜索,     停止动画
     */
    public void stopSearch() {
        rippleBackground.stopRippleAnimation();
    }

    /**
     * 更新数据
     */
    public void updateDeviceList(WifiP2pDeviceList deviceList) {
        for (View view : deviceViewList) {
            rippleBackground.removeView(view);
        }
        deviceViewList.clear();

        List<WifiP2pDevice> peersList = new ArrayList<>();
        peersList.addAll(deviceList.getDeviceList());
        for (WifiP2pDevice device : peersList) {
            add_device(R.drawable.phone2, device);
        }
    }

    /**
     * 添加显示
     */
    public View add_device(int headResource, WifiP2pDevice device) {
        View mDeviceView = View.inflate(this, R.layout.item_device, null);
        mDeviceView.setOnClickListener(new ConnectDeviceListener(device,this));

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
}

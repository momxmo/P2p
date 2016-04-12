package mo.com.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.ckt.io.wifidirect.R;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    public static  String address = "";
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    TextView tv_who;
    TextView tv_state;
    TextView tv_device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_my);
        initView();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this,tv_device);

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("p2p", "发现设备");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i("p2p", "发现设备错误" + reasonCode);
            }
        });
    }

    private void initView() {
        tv_who = (TextView) findViewById(R.id.tv_who);
        tv_state = (TextView) findViewById(R.id.tv_state);
        tv_device = (TextView) findViewById(R.id.tv_device);
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    /**
     * 开启服务端
     *
     * @param view
     */
    public void start_service(View view) {
        tv_who.setText("我是服务端");
        new FileServerAsyncTask(this, tv_state).execute();
    }

    /**
     * 开启客户端
     *
     * @param view
     */
    public void start_client(View view) {
        tv_who.setText("我是客户端");
        new FileClientAsyncTask(this).execute();
    }
}

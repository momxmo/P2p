package com.mo.p2p.activity;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.colorcloud.wifichat.R;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.NetUtils;

import shem.com.materiallogin.MaterialLoginView;
import shem.com.materiallogin.MaterialLoginViewListener;

/**
 * Created by MomxMo on 2016/3/31.
 */
public class Login_activity extends AppCompatActivity {
    MaterialLoginView login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        //注册一个监听连接状态的listener
        EMClient.getInstance().addConnectionListener(new MyConnectionListener());
        initView();
        initEvent();
    }

    private void initView() {
        login = (MaterialLoginView) findViewById(R.id.login);
    }


    //实现ConnectionListener接口
    private void initEvent() {
        login.setListener(new MaterialLoginViewListener() {
            @Override
            public void onRegister(TextInputLayout registerUser, TextInputLayout registerPass, TextInputLayout registerPassRep) {
                String user = registerUser.getEditText().getText().toString();
                if (user.isEmpty()) {
                    registerUser.setError("用户名不能为空");
                    return;
                }
                registerUser.setError("");

                String pass = registerPass.getEditText().getText().toString();
                if (pass.isEmpty()) {
                    registerPass.setError("密码不能为空");
                    return;
                }
                registerPass.setError("");

                String passRep = registerPassRep.getEditText().getText().toString();
                if (!pass.equals(passRep)) {
                    registerPassRep.setError("密码不一致");
                    return;
                }
                registerPassRep.setError("");

                //注册失败会抛出HyphenateException
                try {
                    EMClient.getInstance().createAccount(user, pass);//同步方法
                    Snackbar.make(login, "注册成功!", Snackbar.LENGTH_LONG).show();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                    Snackbar.make(login, "注册失败!", Snackbar.LENGTH_LONG).show();
                }

            }

            @Override
            public void onLogin(TextInputLayout loginUser, final TextInputLayout loginPass) {
                final String userName = loginUser.getEditText().getText().toString();
                String password = loginPass.getEditText().getText().toString();
                if (userName.isEmpty()) {
                    loginUser.setError("用户名不能为空");
                    return;
                }
                loginUser.setError("");
                if (password.isEmpty()) {
                    loginPass.setError("密码不能为空");
                    return;
                }
                loginPass.setError("");
                EMClient.getInstance().login(userName, password, new EMCallBack() {//回调
                    @Override
                    public void onSuccess() {
                        Snackbar.make(login, "登入成功!", Snackbar.LENGTH_LONG).show();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                EMClient.getInstance().groupManager().loadAllGroups();
                                EMClient.getInstance().chatManager().loadAllConversations();
                                Log.d("main", "登陆聊天服务器成功！");
                            }
                        });
                    }

                    @Override
                    public void onProgress(int progress, String status) {
                    }

                    @Override
                    public void onError(int code, String message) {
                        loginPass.setError("密码错误");
                    }
                });
            }
        });
    }

    private class MyConnectionListener implements EMConnectionListener {
        @Override
        public void onConnected() {
            //已连接到服务器
        }

        @Override
        public void onDisconnected(final int error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (error == EMError.USER_REMOVED) {
                        // 显示帐号已经被移除
                    } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                        // 显示帐号在其他设备登陆
                    } else {
                        if (NetUtils.hasNetwork(Login_activity.this)) {
                            //连接不到聊天服务器
                        } else {
                            //当前网络不可用，请检查网络设置
                        }
                    }
                }
            });
        }
    }
}

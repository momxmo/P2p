package com.mo.p2p.activity;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.colorcloud.wifichat.R;
import com.easemob.EMCallBack;
import com.easemob.EMError;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroupManager;
import com.easemob.exceptions.EaseMobException;

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
                final String username = registerUser.getEditText().getText().toString();
                if (username.isEmpty()) {
                    registerUser.setError("用户名不能为空");
                    return;
                }
                registerUser.setError("");

                final String pwd = registerPass.getEditText().getText().toString();
                if (pwd.isEmpty()) {
                    registerPass.setError("密码不能为空");
                    return;
                }
                registerPass.setError("");

                String passRep = registerPassRep.getEditText().getText().toString();
                if (!pwd.equals(passRep)) {
                    registerPassRep.setError("密码不一致");
                    return;
                }
                registerPassRep.setError("");
                register(username, pwd);

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

                Log.d("用户名和密码：", userName+" ： "+password);

                EMChatManager.getInstance().login(userName, password, new EMCallBack() {//回调
                    @Override
                    public void onSuccess() {
                        Snackbar.make(login, "登入成功!", Snackbar.LENGTH_LONG).show();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                EMGroupManager.getInstance().loadAllGroups();
                                EMChatManager.getInstance().loadAllConversations();
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
                        Log.d("main", "登陆聊天服务器失败！");
                    }
                });


            }
        });
    }

    /**
     * 注册
     * @param username
     * @param pwd
     */
    private void register(final String username, final String pwd) {
        new Thread( new Runnable() {
            public void run() {
                try {
                    // 调用sdk注册方法
                    EMChatManager.getInstance().createAccountOnServer(username, pwd);
                } catch (final EaseMobException e) {
                    //注册失败
                    int errorCode=e.getErrorCode();
                    if(errorCode== EMError.NONETWORK_ERROR){
                        Toast.makeText(getApplicationContext(), "网络异常，请检查网络！", Toast.LENGTH_SHORT).show();
                    }else if(errorCode==EMError.USER_ALREADY_EXISTS){
                        Toast.makeText(getApplicationContext(), "用户已存在！", Toast.LENGTH_SHORT).show();
                    }else if(errorCode==EMError.UNAUTHORIZED){
                        Toast.makeText(getApplicationContext(), "注册失败，无权限！", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getApplicationContext(), "注册失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }}).start();
    }

}

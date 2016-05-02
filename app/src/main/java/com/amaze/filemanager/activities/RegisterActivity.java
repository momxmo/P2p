package com.amaze.filemanager.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.EditTextHolder;
import com.easemob.EMError;
import com.easemob.chat.EMChatManager;
import com.easemob.exceptions.EaseMobException;


/**
 * Created by Bob on 2015/2/6.
 */
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener, EditTextHolder.OnEditTextFocusChangeListener, Handler.Callback {


    private static final String TAG = RegisterActivity.class.getSimpleName();
    private static final int HANDLER_REGIST_HAS_NO_FOCUS = 1;
    private static final int HANDLER_REGIST_HAS_FOCUS = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_ac_register);

        registerUser = (EditText) findViewById(R.id.et_register_mail);
        mRegistPassword = (EditText) findViewById(R.id.et_register_password);
        registerPassRep = (EditText) findViewById(R.id.et_register_nickname);
        mRegisteUserAgreement = (TextView) findViewById(R.id.register_user_agreement);
        mRegisteButton = (Button) findViewById(R.id.register_agree_button);
        mRegistReminder = (TextView) findViewById(R.id.de_regist_reminder);
        mLogoImg = (ImageView) findViewById(R.id.de_logo);
        mLeftTitle = (TextView) findViewById(R.id.de_left);
        mRightTitle = (TextView) findViewById(R.id.de_right);
        mImgBackgroud = (ImageView) findViewById(R.id.de_img_backgroud);
        mIsShowTitle = (RelativeLayout) findViewById(R.id.de_merge_rel);
        mEmailDeleteFramelayout = (FrameLayout) findViewById(R.id.et_register_delete);
        mPasswordDeleteFramelayout = (FrameLayout) findViewById(R.id.et_password_delete);
        mNickNameDeleteFramelayout = (FrameLayout) findViewById(R.id.et_nickname_delete);
        mHandler = new Handler(this);
        mSoftManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(RegisterActivity.this, R.anim.translate_anim);
                mImgBackgroud.startAnimation(animation);
            }
        });
        initData();
    }

    protected void initData() {
        mRegisteButton.setOnClickListener(this);
        mRegisteUserAgreement.setOnClickListener(this);
        mLeftTitle.setOnClickListener(this);
        mRightTitle.setOnClickListener(this);
        mEditUserNameEt = new EditTextHolder(registerUser, mEmailDeleteFramelayout, null);
        mEditNickNameEt = new EditTextHolder(registerPassRep, mNickNameDeleteFramelayout, null);
        mEditPassWordEt = new EditTextHolder(mRegistPassword, mPasswordDeleteFramelayout, null);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                registerUser.setOnClickListener(RegisterActivity.this);
                registerPassRep.setOnClickListener(RegisterActivity.this);
                mRegistPassword.setOnClickListener(RegisterActivity.this);
                mEditUserNameEt.setmOnEditTextFocusChangeListener(RegisterActivity.this);
                mEditNickNameEt.setmOnEditTextFocusChangeListener(RegisterActivity.this);
                mEditPassWordEt.setmOnEditTextFocusChangeListener(RegisterActivity.this);
            }
        }, 200);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        event.getKeyCode();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                Message mess = Message.obtain();
                mess.what = HANDLER_REGIST_HAS_NO_FOCUS;
                mHandler.sendMessage(mess);
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
                mSoftManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                Message mess = Message.obtain();
                mess.what = HANDLER_REGIST_HAS_NO_FOCUS;
                mHandler.sendMessage(mess);
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case HANDLER_REGIST_HAS_NO_FOCUS:
                mIsShowTitle.setVisibility(View.GONE);
                mRegistReminder.setVisibility(View.VISIBLE);
                mLogoImg.setVisibility(View.VISIBLE);
                break;
            case HANDLER_REGIST_HAS_FOCUS:
                mLogoImg.setVisibility(View.GONE);
                mRegistReminder.setVisibility(View.GONE);
                mIsShowTitle.setVisibility(View.VISIBLE);
                mLeftTitle.setText(R.string.app_sign_in);
                mRightTitle.setText(R.string.app_fogot_password);
                break;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_agree_button://注册按钮
                final String username = registerUser.getText().toString();
                if (username.isEmpty()) {
                    Snackbar.make(registerUser, "用户名不能为空", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                final String pwd = mRegistPassword.getText().toString();
                if (pwd.isEmpty()) {
                    Snackbar.make(registerUser, "密码不能为空", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                String passRep = registerPassRep.getText().toString();
                if (!pwd.equals(passRep)) {
                    Snackbar.make(registerUser, "密码不一致", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                register(registerPassRep,username, pwd);
                break;
            case R.id.register_user_agreement://用户协议

                break;
            case R.id.et_register_mail:
            case R.id.et_register_password:
            case R.id.et_register_nickname:
                Message mess = Message.obtain();
                mess.what = HANDLER_REGIST_HAS_FOCUS;
                mHandler.sendMessage(mess);
                break;

            case R.id.de_left://��¼
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                break;
            case R.id.de_right:///忘记密码
                Toast.makeText(this, "忘记密码", Toast.LENGTH_SHORT);
                break;
        }
    }

    /**
     * 注册
     *
     * @param username
     * @param pwd
     */
    private void register(final View view,final String username, final String pwd) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // 调用sdk注册方法
                    EMChatManager.getInstance().createAccountOnServer(username, pwd);

                    Snackbar.make(view, "注册成功，赶紧去登入吧！", Snackbar.LENGTH_SHORT).show();
                    finish(); //注册成功

                } catch (final EaseMobException e) {
                    //注册失败
                    int errorCode = e.getErrorCode();
                    String message = null;
                    if (errorCode == EMError.NONETWORK_ERROR) {
                        message = "网络异常，请检查网络！";
                    } else if (errorCode == EMError.USER_ALREADY_EXISTS) {
                        message = "用户已存在！";
                    } else if (errorCode == EMError.UNAUTHORIZED) {
                        message = "注册失败，无权限！";
                    } else {
                        message = "注册失败！";
                    }
                    if (message != null) {
                        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        }).start();
    }


    protected void onPause() {
        super.onPause();
        if (mSoftManager == null) {
            mSoftManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (getCurrentFocus() != null) {
            mSoftManager.hideSoftInputFromWindow(getCurrentFocus()
                    .getWindowToken(), 0);// ���������
        }
    }


    @Override
    public void onEditTextFocusChange(View v, boolean hasFocus) {

        switch (v.getId()) {

            case R.id.et_register_mail:
            case R.id.et_register_password:
            case R.id.et_register_nickname:
                Message mess = Message.obtain();
                if (hasFocus) {
                    mess.what = HANDLER_REGIST_HAS_FOCUS;
                }
                mHandler.sendMessage(mess);
                break;
        }
    }


    /**
     * ע������
     */
    private EditText registerUser;
    /**
     * ����
     */
    private EditText mRegistPassword;
    /**
     * �ǳ�
     */
    private EditText registerPassRep;
    /**
     * ע��button
     */
    private Button mRegisteButton;
    /**
     * �û�Э��
     */
    private TextView mRegisteUserAgreement;
    /**
     * ��������ɾ����ť
     */
    private FrameLayout mEmailDeleteFramelayout;
    /**
     * ��������ɾ����ť
     */
    private FrameLayout mPasswordDeleteFramelayout;
    /**
     * �����ǳ�ɾ����ť
     */
    private FrameLayout mNickNameDeleteFramelayout;
    /**
     * ��ʾ��Ϣ
     */
    private TextView mRegistReminder;
    /**
     * logo
     */
    private ImageView mLogoImg;
    /**
     * ���title
     */
    private TextView mLeftTitle;
    /**
     * �Ҳ�title
     */
    private TextView mRightTitle;
    /**
     * backgroud
     */
    private ImageView mImgBackgroud;
    EditTextHolder mEditUserNameEt;
    EditTextHolder mEditPassWordEt;
    EditTextHolder mEditNickNameEt;
    private Handler mHandler;
    /**
     * ����̵Ŀ���
     */
    private InputMethodManager mSoftManager;
    /**
     * �Ƿ�չʾtitle
     */
    private RelativeLayout mIsShowTitle;
}

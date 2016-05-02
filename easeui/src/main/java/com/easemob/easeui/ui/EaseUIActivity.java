package com.easemob.easeui.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMConversation;
import com.easemob.easeui.EaseConstant;
import com.easemob.easeui.R;
import com.easemob.easeui.domain.EaseUser;
import com.easemob.easeui.ui.EaseContactListFragment.EaseContactListItemClickListener;
import com.easemob.easeui.ui.EaseConversationListFragment.EaseConversationListItemClickListener;
import com.easemob.exceptions.EaseMobException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EaseUIActivity extends EaseBaseActivity {
    private TextView unreadLabel;
    private Button[] mTabs;
    private EaseConversationListFragment conversationListFragment;
    private EaseContactListFragment contactListFragment;
    private Fragment[] fragments;
    private int index;
    private int currentTabIndex;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_main);
        unreadLabel = (TextView) findViewById(R.id.unread_msg_number);
        mTabs = new Button[3];
        mTabs[0] = (Button) findViewById(R.id.btn_conversation);
        mTabs[1] = (Button) findViewById(R.id.btn_address_list);
        // 把第一个tab设为选中状态
        mTabs[0].setSelected(true);

        conversationListFragment = new EaseConversationListFragment();
        contactListFragment = new EaseContactListFragment();
        contactListFragment.setContactsMap(getContacts());
        conversationListFragment.setConversationListItemClickListener(new EaseConversationListItemClickListener() {

            @Override
            public void onListItemClicked(EMConversation conversation) {
                startActivity(new Intent(EaseUIActivity.this, ChatActivity.class).putExtra(EaseConstant.EXTRA_USER_ID, conversation.getUserName()));
            }
        });
        contactListFragment.setContactListItemClickListener(new EaseContactListItemClickListener() {

            @Override
            public void onListItemClicked(EaseUser user) {
                startActivity(new Intent(EaseUIActivity.this, ChatActivity.class).putExtra(EaseConstant.EXTRA_USER_ID, user.getUsername()));
            }
        });
        fragments = new Fragment[]{conversationListFragment, contactListFragment};
        // 添加显示第一个fragment
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, conversationListFragment)
                .add(R.id.fragment_container, contactListFragment).hide(contactListFragment).show(conversationListFragment)
                .commit();

        //登录
        EMChatManager.getInstance().login("2", "1", new EMCallBack() {

            @Override
            public void onSuccess() {
                contactListFragment.setContactsMap(getContacts());
            }

            @Override
            public void onProgress(int progress, String status) {

            }

            @Override
            public void onError(int code, String error) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "登录失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * button点击事件
     *
     * @param view
     */
    public void onTabClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_conversation:
                index = 0;
                break;
            case R.id.btn_address_list:
                index = 1;
                break;
        }
        if (currentTabIndex != index) {
            FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
            trx.hide(fragments[currentTabIndex]);
            if (!fragments[index].isAdded()) {
                trx.add(R.id.fragment_container, fragments[index]);
            }
            trx.show(fragments[index]).commit();
        }
        mTabs[currentTabIndex].setSelected(false);
        // 把当前tab设为选中状态
        mTabs[index].setSelected(true);
        currentTabIndex = index;
    }

    /**
     * 临时生成的数据，密码皆为123456，可以登录测试接发消息
     *
     * @return
     */
    private Map<String, EaseUser> getContacts() {

        EMChatManager.getInstance().getChatOptions().setUseRoster(true);
        Map<String, EaseUser> contacts = new HashMap<String, EaseUser>();
        List<String> usernames = null;
        try {
             usernames = EMContactManager.getInstance().getContactUserNames();
        } catch (EaseMobException e) {
            e.printStackTrace();
        }
        if (usernames != null) {
            for (int i = 1; i < usernames.size(); i++) {
                EaseUser user = new EaseUser(usernames.get(i));
                contacts.put(usernames.get(i), user);
            }
        }

        return contacts;
    }
}

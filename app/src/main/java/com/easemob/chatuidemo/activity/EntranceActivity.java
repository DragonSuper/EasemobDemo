package com.easemob.chatuidemo.activity;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.EMError;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chatuidemo.DemoApplication;
import com.easemob.chatuidemo.R;
import com.easemob.exceptions.EaseMobException;

/**
 * Created by ccheng on 2/10/15.
 */
public class EntranceActivity extends BaseActivity {

    public static final String currentGroupName = "zhuishushenqi";
    private String currentUsername;
    private String currentPassword = "123";
    private String mGroupId;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_entrance);

        currentUsername = generateUserName();
        findViewById(R.id.reg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });

        findViewById(R.id.group).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToGroup(currentGroupName);
            }
        });

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });


    }

    private void login() {

        EMChatManager.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

            @Override
            public void onSuccess() {

                // 登陆成功，保存用户名密码
                DemoApplication.getInstance().setUserName(currentUsername);
                DemoApplication.getInstance().setPassword(currentPassword);

                try {
                    // ** 第一次登录或者之前logout后再登录，加载所有本地群和回话
                    // ** manually load all local groups and
                    // conversations in case we are auto login
                    EMGroupManager.getInstance().loadAllGroups();
                    EMChatManager.getInstance().loadAllConversations();
                } catch (Exception e) {
                    e.printStackTrace();
                    //取好友或者群聊失败，不让进入主页面
                    return;
                }
                //更新当前用户的nickname 此方法的作用是在ios离线推送时能够显示用户nick
                boolean updatenick = EMChatManager.getInstance().updateCurrentUserNick(DemoApplication.currentUserNick.trim());

                // 进入主页面
                Intent intent = new Intent(EntranceActivity.this, ChatActivity.class);
                intent.putExtra("userId", currentUsername);
                if (mGroupId == null) {
                    throw new RuntimeException("Groupid can't be null");
                }

                intent.putExtra("groupId", mGroupId);
                startActivity(intent);
//                finish();
            }


            @Override
            public void onProgress(int progress, String status) {
            }

            @Override
            public void onError(final int code, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(EntranceActivity.this, "failed", Toast.LENGTH_LONG);
                    }
                });
            }
        });
    }

    private void addToGroup(final String groupName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] users = {currentUsername,};
                try {
                    EMGroup publicGroup = EMGroupManager.getInstance().createPublicGroup(groupName, "", null, false);
//                    EMGroupManager.getInstance().inviteUser(groupName, users, null);//异步执行
                    mGroupId = publicGroup.getGroupId();
                } catch (EaseMobException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void register() {
        final String st7 = getResources().getString(R.string.network_anomalies);
        final String st8 = getResources().getString(R.string.User_already_exists);
        final String st9 = getResources().getString(R.string.registration_failed_without_permission);
        final String st10 = getResources().getString(R.string.Registration_failed);
        new Thread(new Runnable() {
            public void run() {
                try {
                    // 调用sdk注册方法
                    EMChatManager.getInstance().createAccountOnServer(currentUsername, currentPassword);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // 保存用户名
                            DemoApplication.getInstance().setUserName(currentUsername);
                            Toast.makeText(getApplicationContext(), String.format("user %s register succeed!", currentUsername), 0).show();
                            finish();
                        }
                    });
                } catch (final EaseMobException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            int errorCode = e.getErrorCode();
                            if (errorCode == EMError.NONETWORK_ERROR) {
                                Toast.makeText(getApplicationContext(), st7, Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ALREADY_EXISTS) {
                                Toast.makeText(getApplicationContext(), st8, Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.UNAUTHORIZED) {
                                Toast.makeText(getApplicationContext(), st9, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), st10 + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();

    }

    private String generateUserName() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress = wInfo.getMacAddress();
        macAddress = macAddress.replaceAll(":", "");
        if (macAddress != null && macAddress.length() >= 8) {
            macAddress = "a" + macAddress.substring(macAddress.length() - 7, macAddress.length());
        }
        return macAddress;
    }
}

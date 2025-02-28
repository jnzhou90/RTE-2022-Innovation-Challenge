package com.qingkouwei.handyinstruction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMChatManager;
import com.hyphenate.chat.EMChatRoomManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMContactManager;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroupManager;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMPushManager;
import com.hyphenate.chat.EMTranslateParams;
import com.hyphenate.cloud.EMHttpClient;
import com.hyphenate.easecallkit.EaseCallKit;
import com.hyphenate.easecallkit.base.EaseCallEndReason;
import com.hyphenate.easecallkit.base.EaseCallKitConfig;
import com.hyphenate.easecallkit.base.EaseCallKitListener;
import com.hyphenate.easecallkit.base.EaseCallKitTokenCallback;
import com.hyphenate.easecallkit.base.EaseCallType;
import com.hyphenate.easecallkit.base.EaseCallUserInfo;
import com.hyphenate.easecallkit.base.EaseGetUserAccountCallback;
import com.hyphenate.easecallkit.base.EaseUserAccount;
import com.hyphenate.easecallkit.event.CallCancelEvent;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.delegate.EaseCustomAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseExpressionAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseFileAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseImageAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseLocationAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseTextAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseVideoAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseVoiceAdapterDelegate;
import com.hyphenate.easeui.domain.EaseAvatarOptions;
import com.hyphenate.easeui.domain.EaseEmojicon;
import com.hyphenate.easeui.domain.EaseEmojiconGroupEntity;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseMessageTypeSetManager;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.provider.EaseEmojiconInfoProvider;
import com.hyphenate.easeui.provider.EaseSettingsProvider;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.push.EMPushHelper;
import com.hyphenate.push.EMPushType;
import com.hyphenate.util.EMLog;
import com.qingkouwei.handyinstruction.common.constant.DemoConstant;
import com.qingkouwei.handyinstruction.common.db.DemoDbHelper;
import com.qingkouwei.handyinstruction.common.livedatas.LiveDataBus;
import com.qingkouwei.handyinstruction.common.manager.UserProfileManager;
import com.qingkouwei.handyinstruction.common.model.DemoModel;
import com.qingkouwei.handyinstruction.common.model.EmojiconExampleGroupData;
import com.qingkouwei.handyinstruction.common.receiver.HeadsetReceiver;
import com.qingkouwei.handyinstruction.common.utils.AppMetaDataHelper;
import com.qingkouwei.handyinstruction.common.utils.FetchUserInfoList;
import com.qingkouwei.handyinstruction.common.utils.FetchUserRunnable;
import com.qingkouwei.handyinstruction.common.utils.PreferenceManager;
import com.qingkouwei.handyinstruction.section.av.MultipleVideoActivity;
import com.qingkouwei.handyinstruction.section.av.VideoCallActivity;
import com.qingkouwei.handyinstruction.section.chat.ChatPresenter;
import com.qingkouwei.handyinstruction.section.chat.delegates.ChatConferenceInviteAdapterDelegate;
import com.qingkouwei.handyinstruction.section.chat.delegates.ChatNotificationAdapterDelegate;
import com.qingkouwei.handyinstruction.section.chat.delegates.ChatRecallAdapterDelegate;
import com.qingkouwei.handyinstruction.section.chat.delegates.ChatUserCardAdapterDelegate;
import com.qingkouwei.handyinstruction.section.chat.delegates.ChatVideoCallAdapterDelegate;
import com.qingkouwei.handyinstruction.section.chat.delegates.ChatVoiceCallAdapterDelegate;
import com.qingkouwei.handyinstruction.section.conference.ConferenceInviteActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * 作为hyphenate-sdk的入口控制类，获取sdk下的基础类均通过此类
 */
public class DemoHelper {
    private static final String TAG = DemoHelper.class.getSimpleName();

    public boolean isSDKInit;//SDK是否初始化
    private static DemoHelper mInstance;
    private DemoModel demoModel = null;
    private Map<String, EaseUser> contactList;
    private UserProfileManager userProManager;

    private EaseCallKitListener callKitListener;
    private Context mainContext;

    private String tokenUrl = "http://a1.easemob.com/token/rtcToken/v1";
    private String uIdUrl = "http://a1.easemob.com/channel/mapper";
    
    private FetchUserRunnable fetchUserRunnable;
    private Thread fetchUserTread;
    private FetchUserInfoList fetchUserInfoList;


    private DemoHelper() {}

    public static DemoHelper getInstance() {
        if(mInstance == null) {
            synchronized (DemoHelper.class) {
                if(mInstance == null) {
                    mInstance = new DemoHelper();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        demoModel = new DemoModel(context);
        //初始化IM SDK
        if(initSDK(context)) {
            // debug mode, you'd better set it to false, if you want release your App officially.
            EMClient.getInstance().setDebugMode(true);
            // set Call options
            setCallOptions(context);
            //初始化推送
            //注册call Receiver
            //initReceiver(context);
            //初始化ease ui相关
            initEaseUI(context);
            //注册对话类型
            registerConversationType();

            //callKit初始化
            InitCallKit(context);

            //启动获取用户信息线程
            fetchUserInfoList = FetchUserInfoList.getInstance();
            fetchUserRunnable = new FetchUserRunnable();
            fetchUserTread = new Thread(fetchUserRunnable);
            fetchUserTread.start();
        }

    }


    /**
     * callKit初始化
     * @param context
     */
    private void InitCallKit(Context context){
        EaseCallKitConfig callKitConfig = new EaseCallKitConfig();
        //设置呼叫超时时间
        callKitConfig.setCallTimeOut(30 * 1000);
        //设置声网AgoraAppId
        callKitConfig.setAgoraAppId(AppMetaDataHelper.getInstance().getPlaceholderValue("AGORA_APPKEY"));
        callKitConfig.setEnableRTCToken(true);
        EaseCallKit.getInstance().init(context,callKitConfig);
        // Register the activities which you have registered in manifest
        EaseCallKit.getInstance().registerVideoCallClass(VideoCallActivity.class);
        EaseCallKit.getInstance().registerMultipleVideoClass(MultipleVideoActivity.class);
        addCallkitListener();
    }

    /**
     * 初始化SDK
     * @param context
     * @return
     */
    private boolean initSDK(Context context) {
        // 根据项目需求对SDK进行配置
        EMOptions options = initChatOptions(context);
        //配置自定义的rest server和im server
//        options.setRestServer("a1-hsb.easemob.com");
//        options.setIMServer("106.75.100.247");
//        options.setImPort(6717);

//        options.setRestServer("a41.easemob.com");
//        options.setIMServer("msync-im-41-tls-test.easemob.com");
//        options.setImPort(6717);

        // 初始化SDK
        isSDKInit = EaseIM.getInstance().init(context, options);
        //设置删除用户属性数据超时时间
        demoModel.setUserInfoTimeOut(30 * 60 * 1000);
        //更新过期用户属性列表
        updateTimeoutUsers();
        mainContext = context;
        return isSDKInit();
    }


    /**
     *注册对话类型
     */
    private void registerConversationType() {
        EaseMessageTypeSetManager.getInstance()
                .addMessageType(EaseExpressionAdapterDelegate.class)       //自定义表情
                .addMessageType(EaseFileAdapterDelegate.class)             //文件
                .addMessageType(EaseImageAdapterDelegate.class)            //图片
                .addMessageType(EaseLocationAdapterDelegate.class)         //定位
                .addMessageType(EaseVideoAdapterDelegate.class)            //视频
                .addMessageType(EaseVoiceAdapterDelegate.class)            //声音
                .addMessageType(ChatConferenceInviteAdapterDelegate.class) //语音邀请
                .addMessageType(ChatRecallAdapterDelegate.class)           //消息撤回
                .addMessageType(ChatVideoCallAdapterDelegate.class)        //视频通话
                .addMessageType(ChatVoiceCallAdapterDelegate.class)        //语音通话
                .addMessageType(ChatUserCardAdapterDelegate.class)         //名片消息
                .addMessageType(EaseCustomAdapterDelegate.class)           //自定义消息
                .addMessageType(ChatNotificationAdapterDelegate.class)     //入群等通知消息
                .setDefaultMessageType(EaseTextAdapterDelegate.class);       //文本
    }

    /**
     * 判断是否之前登录过
     * @return
     */
    public boolean isLoggedIn() {
        return getEMClient().isLoggedInBefore();
    }

    /**
     * 获取IM SDK的入口类
     * @return
     */
    public EMClient getEMClient() {
        return EMClient.getInstance();
    }

    /**
     * 获取contact manager
     * @return
     */
    public EMContactManager getContactManager() {
        return getEMClient().contactManager();
    }

    /**
     * 获取group manager
     * @return
     */
    public EMGroupManager getGroupManager() {
        return getEMClient().groupManager();
    }

    /**
     * 获取chatroom manager
     * @return
     */
    public EMChatRoomManager getChatroomManager() {
        return getEMClient().chatroomManager();
    }


    /**
     * get EMChatManager
     * @return
     */
    public EMChatManager getChatManager() {
        return getEMClient().chatManager();
    }

    /**
     * get push manager
     * @return
     */
    public EMPushManager getPushManager() {
        return getEMClient().pushManager();
    }

    /**
     * get conversation
     * @param username
     * @param type
     * @param createIfNotExists
     * @return
     */
    public EMConversation getConversation(String username, EMConversation.EMConversationType type, boolean createIfNotExists) {
        return getChatManager().getConversation(username, type, createIfNotExists);
    }

    public String getCurrentUser() {
        return getEMClient().getCurrentUser();
    }

    /**
     * ChatPresenter中添加了网络连接状态监听，多端登录监听，群组监听，联系人监听，聊天室监听
     * @param context
     */
    private void initEaseUI(Context context) {
        //添加ChatPresenter,ChatPresenter中添加了网络连接状态监听，
        EaseIM.getInstance().addChatPresenter(ChatPresenter.getInstance());
        EaseIM.getInstance()
                .setSettingsProvider(new EaseSettingsProvider() {
                    @Override
                    public boolean isMsgNotifyAllowed(EMMessage message) {
                        if(message == null){
                            return demoModel.getSettingMsgNotification();
                        }
                        if(!demoModel.getSettingMsgNotification()){
                            return false;
                        }else{
                            String chatUsename = null;
                            List<String> notNotifyIds = null;
                            // get user or group id which was blocked to show message notifications
                            if (message.getChatType() == EMMessage.ChatType.Chat) {
                                chatUsename = message.getFrom();
                                notNotifyIds = demoModel.getDisabledIds();
                            } else {
                                chatUsename = message.getTo();
                                notNotifyIds = demoModel.getDisabledGroups();
                            }

                            if (notNotifyIds == null || !notNotifyIds.contains(chatUsename)) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }

                    @Override
                    public boolean isMsgSoundAllowed(EMMessage message) {
                        return demoModel.getSettingMsgSound();
                    }

                    @Override
                    public boolean isMsgVibrateAllowed(EMMessage message) {
                        return demoModel.getSettingMsgVibrate();
                    }

                    @Override
                    public boolean isSpeakerOpened() {
                        return demoModel.getSettingMsgSpeaker();
                    }
                })
                .setEmojiconInfoProvider(new EaseEmojiconInfoProvider() {
                    @Override
                    public EaseEmojicon getEmojiconInfo(String emojiconIdentityCode) {
                        EaseEmojiconGroupEntity data = EmojiconExampleGroupData.getData();
                        for(EaseEmojicon emojicon : data.getEmojiconList()){
                            if(emojicon.getIdentityCode().equals(emojiconIdentityCode)){
                                return emojicon;
                            }
                        }
                        return null;
                    }

                    @Override
                    public Map<String, Object> getTextEmojiconMapping() {
                        return null;
                    }
                })
                .setAvatarOptions(getAvatarOptions())
                .setUserProvider(new EaseUserProfileProvider() {
                    @Override
                    public EaseUser getUser(String username) {
                        return getUserInfo(username);
                    }

                });
    }

    //Translation Manager 初始化
    public void initTranslationManager() {
        EMTranslateParams params = new EMTranslateParams("46c34219512d4f09ae6f8e04c083b7a3", "https://api.cognitive.microsofttranslator.com", 500);

        EMClient.getInstance().translationManager().init(params);
    }

    /**
     * 统一配置头像
     * @return
     */
    private EaseAvatarOptions getAvatarOptions() {
        EaseAvatarOptions avatarOptions = new EaseAvatarOptions();
        avatarOptions.setAvatarShape(1);
        return avatarOptions;
    }

    public EaseUser getUserInfo(String username) {
        // To get instance of EaseUser, here we get it from the user list in memory
        // You'd better cache it if you get it from your server
        EaseUser user = null;
        if(username.equals(EMClient.getInstance().getCurrentUser()))
            return getUserProfileManager().getCurrentUserInfo();
        user = getContactList().get(username);
        if(user == null){
            //找不到更新会话列表 继续查找
            updateContactList();
            user = getContactList().get(username);
            //如果还找不到从服务端异步拉取 然后通知UI刷新列表
            if(user == null){
                if(fetchUserInfoList != null){
                    fetchUserInfoList.addUserId(username);
                }
            }
        }
        return user;
    }


    /**
     * 根据自己的需要进行配置
     * @param context
     * @return
     */
    private EMOptions initChatOptions(Context context){
        Log.d(TAG, "init HuanXin Options");

        EMOptions options = new EMOptions();
        // 设置是否自动接受加好友邀请,默认是true
        options.setAcceptInvitationAlways(false);
        // 设置是否需要接受方已读确认
        options.setRequireAck(true);
        // 设置是否需要接受方送达确认,默认false
        options.setRequireDeliveryAck(false);
        //设置fpa开关，默认false
        options.setFpaEnable(true);

        //set custom servers, commonly used in private deployment
        if(demoModel.isCustomSetEnable()) {
            if(demoModel.isCustomServerEnable() && demoModel.getRestServer() != null && demoModel.getIMServer() != null) {
                // 设置rest server地址
                options.setRestServer(demoModel.getRestServer());
                // 设置im server地址
                options.setIMServer(demoModel.getIMServer());
                //如果im server地址中包含端口号
                if(demoModel.getIMServer().contains(":")) {
                    options.setIMServer(demoModel.getIMServer().split(":")[0]);
                    // 设置im server 端口号，默认443
                    options.setImPort(Integer.valueOf(demoModel.getIMServer().split(":")[1]));
                }else {
                    //如果不包含端口号
                    if(demoModel.getIMServerPort() != 0) {
                        options.setImPort(demoModel.getIMServerPort());
                    }
                }
            }
        }
        if (demoModel.isCustomAppkeyEnabled() && !TextUtils.isEmpty(demoModel.getCutomAppkey())) {
            // 设置appkey
            options.setAppKey(demoModel.getCutomAppkey());
        }

        String imServer = options.getImServer();
        String restServer = options.getRestServer();

        // 设置是否允许聊天室owner离开并删除会话记录，意味着owner再不会受到任何消息
        options.allowChatroomOwnerLeave(demoModel.isChatroomOwnerLeaveAllowed());
        // 设置退出(主动和被动退出)群组时是否删除聊天消息
        options.setDeleteMessagesAsExitGroup(demoModel.isDeleteMessagesAsExitGroup());
        // 设置是否自动接受加群邀请
        options.setAutoAcceptGroupInvitation(demoModel.isAutoAcceptGroupInvitation());
        // 是否自动将消息附件上传到环信服务器，默认为True是使用环信服务器上传下载
        options.setAutoTransferMessageAttachments(demoModel.isSetTransferFileByUser());
        // 是否自动下载缩略图，默认是true为自动下载
        options.setAutoDownloadThumbnail(demoModel.isSetAutodownloadThumbnail());
        return options;
    }

    private void setCallOptions(Context context) {
        HeadsetReceiver headsetReceiver = new HeadsetReceiver();
        IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(headsetReceiver, headsetFilter);
    }

    /**
     * logout
     *
     * @param unbindDeviceToken
     *            whether you need unbind your device token
     * @param callback
     *            callback
     */
    public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
        Log.d(TAG, "logout: " + unbindDeviceToken);
        if(fetchUserTread != null && fetchUserRunnable != null){
            fetchUserRunnable.setStop(true);
        }

        CallCancelEvent cancelEvent = new CallCancelEvent();
        EaseCallKit.getInstance().sendCmdMsg(cancelEvent, EaseCallKit.getInstance().getFromUserId(), new EMCallBack() {
            @Override
            public void onSuccess() {
                EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {

                    @Override
                    public void onSuccess() {
                        logoutSuccess();
                        //reset();
                        if (callback != null) {
                            callback.onSuccess();
                        }

                    }

                    @Override
                    public void onProgress(int progress, String status) {
                        if (callback != null) {
                            callback.onProgress(progress, status);
                        }
                    }

                    @Override
                    public void onError(int code, String error) {
                        Log.d(TAG, "logout: onSuccess");
                        //reset();
                        if (callback != null) {
                            callback.onError(code, error);
                        }
                    }
                });
            }

            @Override
            public void onError(int code, String error) {
                EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {

                    @Override
                    public void onSuccess() {
                        logoutSuccess();
                        //reset();
                        if (callback != null) {
                            callback.onSuccess();
                        }

                    }

                    @Override
                    public void onProgress(int progress, String status) {
                        if (callback != null) {
                            callback.onProgress(progress, status);
                        }
                    }

                    @Override
                    public void onError(int code, String error) {
                        Log.d(TAG, "logout: onSuccess");
                        //reset();
                        if (callback != null) {
                            callback.onError(code, error);
                        }
                    }
                });
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
    }

    /**
     * 关闭当前进程
     */
    public void killApp() {
        List<Activity> activities = DemoApplication.getInstance().getLifecycleCallbacks().getActivityList();
        if(activities != null && !activities.isEmpty()) {
            for(Activity activity : activities) {
                activity.finish();
            }
        }
        Process.killProcess(Process.myPid());
        System.exit(0);
    }



    /**
     * 退出登录后，需要处理的业务逻辑
     */
    public void logoutSuccess() {
        Log.d(TAG, "logout: onSuccess");
        setAutoLogin(false);
        DemoDbHelper.getInstance(DemoApplication.getInstance()).closeDb();
        getUserProfileManager().reset();
        EMClient.getInstance().translationManager().logout();
    }

    public EaseAvatarOptions getEaseAvatarOptions() {
        return EaseIM.getInstance().getAvatarOptions();
    }

    public DemoModel getModel(){
        if(demoModel == null) {
            demoModel = new DemoModel(DemoApplication.getInstance());
        }
        return demoModel;
    }

    public String getCurrentLoginUser() {
        return getModel().getCurrentUsername();
    }

    /**
     * get instance of EaseNotifier
     * @return
     */
    public EaseNotifier getNotifier(){
        return EaseIM.getInstance().getNotifier();
    }

    /**
     * 设置本地标记，是否自动登录
     * @param autoLogin
     */
    public void setAutoLogin(boolean autoLogin) {
        PreferenceManager.getInstance().setAutoLogin(autoLogin);
    }

    /**
     * 获取本地标记，是否自动登录
     * @return
     */
    public boolean getAutoLogin() {
        return PreferenceManager.getInstance().getAutoLogin();
    }

    /**
     * 设置SDK是否初始化
     * @param init
     */
    public void setSDKInit(boolean init) {
        isSDKInit = init;
    }

    public boolean isSDKInit() {
        return isSDKInit;
    }

    /**
     * 向数据库中插入数据
     * @param object
     */
    public void insert(Object object) {
        demoModel.insert(object);
    }

    /**
     * update
     * @param object
     */
    public void update(Object object) {
        demoModel.update(object);
    }

    /**
     * update user list
     * @param users
     */
    public void updateUserList(List<EaseUser> users){
        demoModel.updateContactList(users);
    }

    /**
     * 更新过期的用户属性数据
     */
    public void updateTimeoutUsers() {
        List<String> userIds = demoModel.selectTimeOutUsers();
        if(userIds != null && userIds.size() > 0){
            if(fetchUserInfoList != null){
                for(int i = 0; i < userIds.size(); i++){
                    fetchUserInfoList.addUserId(userIds.get(i));
                }
            }
        }
    }

    /**
     * get contact list
     *
     * @return
     */
    public Map<String, EaseUser> getContactList() {
        if (isLoggedIn() && contactList == null) {
            updateTimeoutUsers();
            contactList = demoModel.getAllUserList();
        }

        // return a empty non-null object to avoid app crash
        if(contactList == null){
            return new Hashtable<String, EaseUser>();
        }
        return contactList;
    }

    /**
     * update contact list
     */
    public void updateContactList() {
        if(isLoggedIn()) {
            updateTimeoutUsers();
            contactList = demoModel.getContactList();
        }
    }

    public UserProfileManager getUserProfileManager() {
        if (userProManager == null) {
            userProManager = new UserProfileManager();
        }
        return userProManager;
    }

    /**
     * 删除联系人
     * @param username
     * @return
     */
    public synchronized int deleteContact(String username) {
        if(TextUtils.isEmpty(username)) {
            return 0;
        }
        DemoDbHelper helper = DemoDbHelper.getInstance(DemoApplication.getInstance());
        if(helper.getUserDao() == null) {
            return 0;
        }
        int num = helper.getUserDao().deleteUser(username);
        if(helper.getInviteMessageDao() != null) {
            helper.getInviteMessageDao().deleteByFrom(username);
        }
        EMClient.getInstance().chatManager().deleteConversation(username, false);
        getModel().deleteUsername(username, false);
        Log.e(TAG, "delete num = "+num);
        return num;
    }

    /**
     * 检查是否是第一次安装登录
     * 默认值是true, 需要在用api拉取完会话列表后，就其置为false.
     * @return
     */
    public boolean isFirstInstall() {
        return getModel().isFirstInstall();
    }

    /**
     * 将状态置为非第一次安装，在调用获取会话列表的api后调用
     * 并将会话列表是否来自服务器置为true
     */
    public void makeNotFirstInstall() {
        getModel().makeNotFirstInstall();
    }

    /**
     * 检查会话列表是否从服务器返回数据
     * @return
     */
    public boolean isConComeFromServer() {
        return getModel().isConComeFromServer();
    }

    /**
     * Determine if it is from the current user account of another device
     * @param username
     * @return
     */
    public boolean isCurrentUserFromOtherDevice(String username) {
        if(TextUtils.isEmpty(username)) {
            return false;
        }
        if(username.contains("/") && username.contains(EMClient.getInstance().getCurrentUser())) {
            return true;
        }
        return false;
    }


    /**
     * 增加EaseCallkit监听
     *
     */
    public void addCallkitListener(){
        callKitListener = new EaseCallKitListener() {
            @Override
            public void onInviteUsers(Context context, String userId[], JSONObject ext) {
                Intent intent = new Intent(context, ConferenceInviteActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                String groupId = null;
                if(ext != null && ext.length() > 0){
                    try {
                        groupId = ext.getString("groupId");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                intent.putExtra(DemoConstant.EXTRA_CONFERENCE_GROUP_ID, groupId);
                intent.putExtra(DemoConstant.EXTRA_CONFERENCE_GROUP_EXIST_MEMBERS, userId);
                context.startActivity(intent);
            }

            @Override
            public void onEndCallWithReason(EaseCallType callType, String channelName, EaseCallEndReason reason, long callTime) {
                EMLog.d(TAG,"onEndCallWithReason" + (callType != null ? callType.name() : " callType is null ") + " reason:" + reason + " time:"+ callTime);
                SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String callString = mainContext.getString(R.string.call_duration);
                callString += formatter.format(callTime);

                Toast.makeText(mainContext,callString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGenerateToken(String userId, String channelName, String appKey, EaseCallKitTokenCallback callback){
                EMLog.d(TAG,"onGenerateToken userId:" + userId + " channelName:" + channelName + " appKey:"+ appKey);
                String url = tokenUrl;
                url += "?";
                url += "userAccount=";
                url += userId;
                url += "&channelName=";
                url += channelName;
                url += "&appkey=";
                url +=  appKey;

                //获取声网Token
                getRtcToken(url, callback);
            }

            @Override
            public void onReceivedCall(EaseCallType callType, String fromUserId, JSONObject ext) {
                //收到接听电话
                EMLog.d(TAG,"onRecivedCall" + callType.name() + " fromUserId:" + fromUserId);
            }
            @Override
            public  void onCallError(EaseCallKit.EaseCallError type, int errorCode, String description){

            }

            @Override
            public void onInViteCallMessageSent(){
                LiveDataBus.get().with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(DemoConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
            }

            @Override
            public void onRemoteUserJoinChannel(String channelName, String userName, int uid, EaseGetUserAccountCallback callback){
                if(userName == null || userName == ""){
                    String url = uIdUrl;
                    url += "?";
                    url += "channelName=";
                    url += channelName;
                    url += "&userAccount=";
                    url += EMClient.getInstance().getCurrentUser();
                    url += "&appkey=";
                    url +=  EMClient.getInstance().getOptions().getAppKey();
                    getUserIdAgoraUid(uid,url,callback);
                }else{
                    //设置用户昵称 头像
                    setEaseCallKitUserInfo(userName);
                    EaseUserAccount account = new EaseUserAccount(uid,userName);
                    List<EaseUserAccount> accounts = new ArrayList<>();
                    accounts.add(account);
                    callback.onUserAccount(accounts);
                }
            }
        };
        EaseCallKit.getInstance().setCallKitListener(callKitListener);
    }


    /**
     * 获取声网Token
     *
     */
    private void getRtcToken(String tokenUrl,EaseCallKitTokenCallback callback){
        new AsyncTask<String, Void, Pair<Integer, String>>(){
            @Override
            protected Pair<Integer, String> doInBackground(String... str) {
                try {
                    Pair<Integer, String>
                        response = EMHttpClient.getInstance().sendRequestWithToken(tokenUrl, null,EMHttpClient.GET);
                    return response;
                }catch (HyphenateException exception) {
                    exception.printStackTrace();
                }
                return  null;
            }
            @Override
            protected void onPostExecute(Pair<Integer, String> response) {
                if(response != null) {
                    try {
                          int resCode = response.first;
                          if(resCode == 200){
                              String responseInfo = response.second;
                              if(responseInfo != null && responseInfo.length() > 0){
                                  try {
                                      JSONObject object = new JSONObject(responseInfo);
                                      String token = object.getString("accessToken");
                                      int uId = object.getInt("agoraUserId");

                                      //设置自己头像昵称
                                      setEaseCallKitUserInfo(EMClient.getInstance().getCurrentUser());
                                      callback.onSetToken(token,uId);
                                  }catch (Exception e){
                                      e.getStackTrace();
                                  }
                              }else{
                                  callback.onGetTokenError(response.first,response.second);
                              }
                          }else{
                              callback.onGetTokenError(response.first,response.second);
                          }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    callback.onSetToken(null,0);
                }
            }
        }.execute(tokenUrl);
    }

    /**
     * 根据channelName和声网uId获取频道内所有人的UserId
     * @param uId
     * @param url
     * @param callback
     */
    private void getUserIdAgoraUid(int uId, String url, EaseGetUserAccountCallback callback){
        new AsyncTask<String, Void, Pair<Integer, String>>(){
            @Override
            protected Pair<Integer, String> doInBackground(String... str) {
                try {
                    Pair<Integer, String>
                        response = EMHttpClient.getInstance().sendRequestWithToken(url, null,EMHttpClient.GET);
                    return response;
                }catch (HyphenateException exception) {
                    exception.printStackTrace();
                }
                return  null;
            }
            @Override
            protected void onPostExecute(Pair<Integer, String> response) {
                if(response != null) {
                    try {
                        int resCode = response.first;
                        if(resCode == 200){
                            String responseInfo = response.second;
                            List<EaseUserAccount> userAccounts = new ArrayList<>();
                            if(responseInfo != null && responseInfo.length() > 0){
                                try {
                                    JSONObject object = new JSONObject(responseInfo);
                                    JSONObject resToken = object.getJSONObject("result");
                                    Iterator it = resToken.keys();
                                    while(it.hasNext()) {
                                        String uIdStr = it.next().toString();
                                        int uid = 0;
                                        uid = Integer.valueOf(uIdStr).intValue();
                                        String username = resToken.optString(uIdStr);
                                        if(uid == uId){
                                            //获取到当前用户的userName 设置头像昵称等信息
                                            setEaseCallKitUserInfo(username);
                                        }
                                        userAccounts.add(new EaseUserAccount(uid, username));
                                    }
                                    callback.onUserAccount(userAccounts);
                                }catch (Exception e){
                                    e.getStackTrace();
                                }
                            }else{
                                callback.onSetUserAccountError(response.first,response.second);
                            }
                        }else{
                            callback.onSetUserAccountError(response.first,response.second);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    callback.onSetUserAccountError(100,"response is null");
                }
            }
        }.execute(url);
    }


    /**
     * 设置callKit 用户头像昵称
     * @param userName
     */
    private void setEaseCallKitUserInfo(String userName){
        EaseUser user = getUserInfo(userName);
        EaseCallUserInfo userInfo = new EaseCallUserInfo();
        if(user != null){
            userInfo.setNickName(user.getNickname());
            userInfo.setHeadImage(user.getAvatar());
        }
        EaseCallKit.getInstance().getCallKitConfig().setUserInfo(userName,userInfo);
    }


    /**
     * data sync listener
     */
    public interface DataSyncListener {
        /**
         * sync complete
         * @param success true：data sync successful，false: failed to sync data
         */
        void onSyncComplete(boolean success);
    }
}

/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.spi;

import cn.wildfirechat.pojos.SystemSettingPojo;
import cn.wildfirechat.proto.WFCMessage;
import com.secret.loServer.model.FriendData;
import cn.wildfirechat.pojos.InputOutputUserBlockStatus;
import io.moquette.persistence.DatabaseStore;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.persistence.UserClientEntry;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import cn.wildfirechat.common.ErrorCode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages
 */
public interface IMessagesStore {

    /**
     * 2021年3月23日时间 app 2.3.0增加
     * @param fromUser 某人
     * @param requestList 要添加的人列表
     * @return 是否添加成功
     */
    ErrorCode addFriendSoon(String fromUser, List<WFCMessage.UserRequest> requestList,Long addUserTimeMillis);

    /**
     * 2021年3月25日17点00分 app 2.3.0增加x
     * @param userId 某人
     * @return
     */
    List<String> getCommonFriendIdList(String userId);

    /**
     * 2021年3月25日21点21分 app2.3.0增加
     * @param userId 某人用户code
     */
    List<String> getUserNotAddFriendList(String userId);

    /**
     * 2021年4月15日15点27分 app 2.3.0增加  单向删除某人好友
     * @param userId 某人
     * @param friendUid 删除对方的id
     * @param head
     * @return
     */
    ErrorCode deleteFriendByOneWay(String userId, String friendUid, long[] head);

    void insertOrUpdateUserToken(String clientId, String userId, String token);

    Map<String,String> findUserTokenByUserId(String userName);

    void insertOrUpdateUserTokenByClientId(String clientId, String userId, String token);


    class StoredMessage {

        final MqttQoS m_qos;
        final byte[] m_payload;
        final String m_topic;
        private boolean m_retained;
        private String m_clientID;
        private MessageGUID m_guid;

        public StoredMessage(byte[] message, MqttQoS qos, String topic) {
            m_qos = qos;
            m_payload = message;
            m_topic = topic;
        }

        public MqttQoS getQos() {
            return m_qos;
        }

        public String getTopic() {
            return m_topic;
        }

        public void setGuid(MessageGUID guid) {
            this.m_guid = guid;
        }

        public MessageGUID getGuid() {
            return m_guid;
        }

        public String getClientID() {
            return m_clientID;
        }

        public void setClientID(String m_clientID) {
            this.m_clientID = m_clientID;
        }

        public ByteBuf getPayload() {
            return Unpooled.copiedBuffer(m_payload);
        }

        public void setRetained(boolean retained) {
            this.m_retained = retained;
        }

        public boolean isRetained() {
            return m_retained;
        }

        @Override
        public String toString() {
            return "PublishEvent{clientID='" + m_clientID + '\'' + ", m_retain="
                    + m_retained + ", m_qos=" + m_qos + ", m_topic='" + m_topic + '\'' + '}';
        }
    }

    DatabaseStore getDatabaseStore();

    WFCMessage.Message storeMessage(String fromUser, String fromClientId, WFCMessage.Message message);

    void storeSensitiveMessage(WFCMessage.Message message);

    int getNotifyReceivers(String fromUser, WFCMessage.Message.Builder message, Set<String> notifyReceivers, boolean ignoreMsg);

    Set<String> getAllEnds();

    WFCMessage.PullMessageResult fetchMessage(String user, String exceptClientId, long fromMessageId, int pullType);


    /**
     * 拉取历史消息
     *
     * @param user
     * @param conversation
     * @param beforeUid
     * @param count
     * @return
     */
    WFCMessage.PullMessageResult loadRemoteMessages(String user, WFCMessage.Conversation conversation, long beforeUid, int count);

    long insertUserMessages(String sender, int conversationType, String target, int line, int messageContentType, String userId, long messageId);

    /**
     * 创建群聊
     *
     * @param operator
     * @param groupInfo
     * @param memberList
     * @return
     */
    WFCMessage.GroupInfo createGroup(String operator, WFCMessage.GroupInfo groupInfo, List<WFCMessage.GroupMember> memberList);

    /**
     * 添加群聊成员
     *
     * @param operator
     * @param isAdmin
     * @param groupId
     * @param memberList
     * @return
     */
    ErrorCode addGroupMembers(String operator, boolean isAdmin, String groupId, List<WFCMessage.GroupMember> memberList);

    ErrorCode kickoffGroupMembers(String operator, boolean isAdmin, String groupId, List<String> memberList);

    ErrorCode quitGroup(String operator, String groupId);

    void clearUserGroups(String userId);

    ErrorCode dismissGroup(String operator, String groupId, boolean isAdmin);

    ErrorCode modifyGroupInfo(String operator, String groupId, int modifyType, String value, boolean isAdmin);

    ErrorCode modifyGroupAlias(String operator, String groupId, String alias);

    /**
     * 获取聊天群详情
     *
     * @param requests
     * @return
     */
    List<WFCMessage.GroupInfo> getGroupInfos(List<WFCMessage.UserRequest> requests);

    WFCMessage.GroupInfo getGroupInfo(String groupId);

    Set<String> getUserGroupIds(String userId);

    ErrorCode getGroupMembers(String groupId, long maxDt, List<WFCMessage.GroupMember> members);

    WFCMessage.GroupMember getGroupMember(String groupId, String memberId);

    ErrorCode transferGroup(String operator, String groupId, String newOwner, boolean isAdmin);

    ErrorCode setGroupManager(String operator, String groupId, int type, List<String> userList, boolean isAdmin);

    boolean isMemberInGroup(String member, String groupId);

    ErrorCode canSendMessageInGroup(String member, String groupId);

    ErrorCode recallMessage(long messageUid, String operatorId, String clientId, boolean isAdmin);

    void clearUserMessages(String userId);

    WFCMessage.Robot getRobot(String robotId);

    void addRobot(WFCMessage.Robot robot);

    /**
     * 获取用户详情
     *
     * @param requestList
     * @param builder
     * @return
     */
    ErrorCode getUserInfo(List<WFCMessage.UserRequest> requestList, WFCMessage.PullUserResult.Builder builder);

    ErrorCode modifyUserInfo(String userId, WFCMessage.ModifyMyInfoRequest request) throws Exception;

    void updateUserOnlineSetting(MemorySessionStore.Session session, boolean online);

    ErrorCode modifyUserStatus(String userId, int status);

    int getUserStatus(String userId);

    List<InputOutputUserBlockStatus> getUserStatusList();

    void addUserInfo(WFCMessage.User user, String password) throws Exception;

    void destoryUser(String userId);

    WFCMessage.User getUserInfo(String userId);

    WFCMessage.User getUserInfoByName(String name);

    WFCMessage.User getUserInfoByMobile(String mobile);

    List<WFCMessage.User> searchUser(String keyword, int searchType, int page);

    boolean updateSystemSetting(int id, String value, String desc);

    SystemSettingPojo getSystemSetting(int id);

    void createChatroom(String chatroomId, WFCMessage.ChatroomInfo chatroomInfo);

    void destoryChatroom(String chatroomId);

    WFCMessage.ChatroomInfo getChatroomInfo(String chatroomId);

    WFCMessage.ChatroomMemberInfo getChatroomMemberInfo(String chatroomId, final int maxMemberCount);

    int getChatroomMemberCount(String chatroomId);

    Collection<String> getChatroomMemberClient(String userId);

    boolean checkUserClientInChatroom(String user, String clientId, String chatroomId);

    long insertChatroomMessages(String target, int line, long messageId);

    Collection<UserClientEntry> getChatroomMembers(String chatroomId);

    WFCMessage.PullMessageResult fetchChatroomMessage(String fromUser, String chatroomId, String exceptClientId, long fromMessageId);

    ErrorCode verifyToken(String userId, String token, List<String> serverIPs, List<Integer> ports);

    ErrorCode login(String name, String password, List<String> userIdRet);

    List<FriendData> getFriendList(String userId, String clientId, long version);

    void clearUserFriend(String userId);

    List<WFCMessage.FriendRequest> getFriendRequestList(String userId, long version);

    /**
     * 保存好友请求信息
     *
     * @param userId
     * @param request
     * @param head
     * @return
     */
    ErrorCode saveAddFriendRequest(String userId, WFCMessage.AddFriendRequest request, long[] head);

    ErrorCode handleFriendRequest(String userId, WFCMessage.HandleFriendRequest request, WFCMessage.Message.Builder msgBuilder, long[] heads, boolean isAdmin);

    ErrorCode handlePublishFriendRequest(String userId, WFCMessage.HandleFriendRequest request, WFCMessage.Message.Builder msgBuilder, long[] heads);

    /**
     * 删除好友
     *
     * @param userId
     * @param friendUid
     * @param head
     * @return
     */
    ErrorCode deleteFriend(String userId, String friendUid, long[] head);

    ErrorCode blackUserRequest(String fromUser, String targetUserId, int status, long[] head);

    FriendData getFriendData(String fromUser, String targetUserId);

    ErrorCode SyncFriendRequestUnread(String userId, long unreadDt, long[] head);

    ErrorCode isAllowUserMessage(String fromUser, String userId);

    ErrorCode setFriendAliasRequest(String fromUser, String targetUserId, String alias, long[] head);

    ErrorCode handleJoinChatroom(String userId, String clientId, String chatroomId);

    ErrorCode handleQuitChatroom(String userId, String clientId, String chatroomId);

    boolean checkChatroomParticipantIdelTime(MemorySessionStore.Session session);

    ErrorCode getUserSettings(String userId, long version, WFCMessage.GetUserSettingResult.Builder builder);

    WFCMessage.UserSettingEntry getUserSetting(String userId, int scope, String key);

    List<WFCMessage.UserSettingEntry> getUserSetting(String userId, int scope);

    long updateUserSettings(String userId, WFCMessage.ModifyUserSettingReq request);

    void clearUserSettings(String userId);

    boolean getUserGlobalSlient(String userId);

    boolean getUserPushHiddenDetail(String userId);

    boolean getUserConversationSlient(String userId, WFCMessage.Conversation conversation);

    ErrorCode createChannel(String operator, WFCMessage.ChannelInfo channelInfo);

    void clearUserChannels(String userId);

    ErrorCode modifyChannelInfo(String operator, String channelId, int modifyType, String value);

    ErrorCode transferChannel(String operator, String channelId, String newOwner);

    ErrorCode distoryChannel(String operator, String channelId);

    List<WFCMessage.ChannelInfo> searchChannel(String keyword, boolean buzzy, int page);

    ErrorCode listenChannel(String operator, String channelId, boolean listen);

    WFCMessage.ChannelInfo getChannelInfo(String channelId);

    boolean checkUserInChannel(String user, String channelId);

    Collection<String> getChannelSubscriber(String channelId);

    Set<String> handleSensitiveWord(String message);

    boolean addSensitiveWords(List<String> words);

    boolean removeSensitiveWords(List<String> words);

    List<String> getAllSensitiveWords();

    WFCMessage.Message getMessage(long messageId);

    long getMessageHead(String user);

    long getAndIncreaseMessageHead(String user);

    long getFriendHead(String user);

    long getFriendRqHead(String user);

    long getSettingHead(String user);

    //使用了数据库，会比较慢，仅能用户用户/群组等id的生成
    String getShortUUID();

    /**
     * Used to initialize all persistent store structures
     */
    void initStore();

    /**
     * Return a list of retained messages that satisfy the condition.
     *
     * @param condition the condition to match during the search.
     * @return the collection of matching messages.
     */
    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    void cleanRetained(Topic topic);

    void storeRetained(Topic topic, StoredMessage storedMessage);
}

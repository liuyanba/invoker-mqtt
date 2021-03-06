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

package io.moquette.spi.impl;

import cn.secret.util.ProtoUtil;
import cn.wildfirechat.pojos.OutputMessageData;
import cn.wildfirechat.pojos.OutputNotifyChannelSubscribeStatus;
import cn.wildfirechat.pojos.SendMessageData;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.push.PushServer;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.util.StringUtil;
import com.secret.loServer.model.FriendData;
import io.moquette.persistence.MemorySessionStore.Session;
import io.moquette.persistence.UserClientEntry;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.HttpUtils;
import com.liuyan.im.IMTopic;
import com.liuyan.im.Utility;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import static cn.wildfirechat.proto.ProtoConstants.PersistFlag.Transparent;
import static io.moquette.BrokerConstants.NODE_ID;
import static io.moquette.liuyan.contants.LaoLiuConstants.HazelcastConstants.NON_EXIST_NODE;

/**
 * ????????????
 */
public class MessagesPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MessagesPublisher.class);
    private final ConnectionDescriptorStore connectionDescriptors;
    private final ISessionsStore m_sessionsStore;
    private final IMessagesStore m_messagesStore;
    private final PersistentQueueMessageSender messageSender;
    private ConcurrentHashMap<UserClientEntry, Long> chatRoomHeaders = new ConcurrentHashMap<>();
    private ExecutorService chatroomScheduler = Executors.newFixedThreadPool(1);
    private boolean schedulerStarted = false;
    private static ExecutorService executorCallback = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    //TODO: 2020???12???8???20???06?????????
    private final HazelcastInstance hazelcastInstance;

    public void startChatroomScheduler() {
        schedulerStarted = true;
        chatroomScheduler.execute(() -> {
            while (schedulerStarted) {
                try {
                    if (chatRoomHeaders.size() < 100) {
                        Thread.sleep(500);
                    } else if (chatRoomHeaders.size() < 500) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }

                chatRoomHeaders.forEach(100, (s, aLong) -> {
                    chatRoomHeaders.remove(s, aLong);
                    publish2ChatroomReceivers(s.userId, s.clientId, aLong);
                });
            }
        });
    }

    public void stopChatroomScheduler() {
        schedulerStarted = false;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
        chatroomScheduler.shutdown();
    }

    private final BrokerInterceptor m_interceptor;//????????????????????????

    public MessagesPublisher(ConnectionDescriptorStore connectionDescriptors, ISessionsStore sessionsStore,
                             PersistentQueueMessageSender messageSender, HazelcastInstance hz, IMessagesStore messagesStore, BrokerInterceptor interceptor) {
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
        this.messageSender = messageSender;
        this.m_messagesStore = messagesStore;
        this.hazelcastInstance = hz;
        this.m_interceptor = interceptor;
        this.startChatroomScheduler();
    }

    static MqttPublishMessage notRetainedPublish(String topic, MqttQoS qos, ByteBuf message) {
        return notRetainedPublishWithMessageId(topic, qos, message, 0);
    }

    private static MqttPublishMessage notRetainedPublishWithMessageId(String topic, MqttQoS qos, ByteBuf message,
                                                                      int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }

    private void publish2ChatroomReceivers(String user, String clientId, long messageHead) {
        publish2ChatroomReceiversDirectly(user, clientId, messageHead);
    }

    public void publish2ChatroomReceiversDirectly(String user, String clientId, long messageHead) {
        try {
            Session session = m_sessionsStore.getSession(clientId);

            if (session != null) {
                LOG.warn("?????? {} ?????????", clientId);
                return;
            }
            if (!session.getUsername().equals(user)) {
                LOG.warn("?????? {} ???????????? {} ", clientId, user);
                return;
            }
            if (!this.connectionDescriptors.isConnected(clientId)) {
                LOG.warn("?????? {} ?????????", clientId);
                return;
            }
            //????????????
            WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage
                    .newBuilder()
                    .setType(ProtoConstants.PullType.Pull_ChatRoom)
                    .setHead(messageHead)
                    .build();

            ByteBuf payload = Unpooled.buffer();
            byte[] byteData = notifyMessage.toByteArray();
            payload.ensureWritable(byteData.length).writeBytes(byteData);
            MqttPublishMessage publishMsg;
            publishMsg = notRetainedPublish(IMTopic.NotifyMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

            boolean result = !this.messageSender.sendPublish(session.getClientSession(), publishMsg);
            if (!result) {
                LOG.warn("??????????????? {} ??????", clientId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    /**
     * @param sender
     * @param conversationType
     * @param target
     * @param line
     * @param messageHead
     * @param receivers
     * @param pushContent
     * @param exceptClientId
     * @param pullType
     * @param messageContentType
     * @param serverTime
     * @param mentionType
     * @param mentionTargets
     * @param persistFlag
     */
    private void publish2Receivers(String sender, int conversationType, String target, int line, long messageHead, Collection<String> receivers, String pushContent, String exceptClientId, int pullType, int messageContentType, long serverTime, int mentionType, List<String> mentionTargets, int persistFlag) {
        if (persistFlag == Transparent) {
            publishTransparentMessage2Receivers(messageHead, receivers, pullType);
            return;
        }

        WFCMessage.Message message = null;
        for (String user : receivers) {
            if (!user.equals(sender)) {
                WFCMessage.User userInfo = m_messagesStore.getUserInfo(user);
                if (userInfo != null && userInfo.getType() == ProtoConstants.UserType.UserType_Robot) {
                    WFCMessage.Robot robot = m_messagesStore.getRobot(user);
                    if (robot != null && !StringUtil.isNullOrEmpty(robot.getCallback())) {
                        if (message == null) {
                            message = m_messagesStore.getMessage(messageHead);
                        }
                        final WFCMessage.Message finalMsg = message;
                        executorCallback.execute(() -> HttpUtils.httpJsonPost(robot.getCallback(), new Gson().toJson(SendMessageData.fromProtoMessage(finalMsg), SendMessageData.class)));
                        continue;
                    }
                }
            }
            //TODO ????????????
            long messageSeq;
            if (pullType != ProtoConstants.PullType.Pull_ChatRoom) {
                messageSeq = m_messagesStore.insertUserMessages(sender, conversationType, target, line, messageContentType, user, messageHead);
            } else {
                messageSeq = m_messagesStore.insertChatroomMessages(user, line, messageHead);
            }

            Collection<Session> sessions = m_sessionsStore.sessionForUser(user);
            String senderName = null;
            String targetName = null;
            boolean nameLoaded = false;


            Collection<String> targetClients = null;
            if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                targetClients = m_messagesStore.getChatroomMemberClient(user);
            }
            for (Session targetSession : sessions) {
                //TODO ??????7???????????????????????????
                if (System.currentTimeMillis() - targetSession.getLastActiveTime() > 7 * 24 * 60 * 60 * 1000) {
                    LOG.info("??????7?????????????????????????????????  ClientId:{}  ??????????????????:{}", targetSession.getClientID(), targetSession.getLastActiveTime());
                    continue;
                }

                if (exceptClientId != null && exceptClientId.equals(targetSession.getClientSession().clientID)) {
                    LOG.info("???????????????clientId????????????????????????????????????:{}   ?????????:{}", exceptClientId, targetSession.getClientID());
                    continue;
                }

                if (targetSession.getClientID() == null) {
                    LOG.info("????????????????????????Id?????????????????????:{}", targetSession.getClientID());
                    continue;
                }

                if (pullType == ProtoConstants.PullType.Pull_ChatRoom && !targetClients.contains(targetSession.getClientID())) {
                    continue;
                }

                if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                    if (exceptClientId != null && exceptClientId.equals(targetSession.getClientID())) {
                        targetSession.refreshLastChatroomActiveTime();
                    }

                    if (!m_messagesStore.checkChatroomParticipantIdelTime(targetSession)) {
                        m_messagesStore.handleQuitChatroom(user, targetSession.getClientID(), target);
                        continue;
                    }
                }

                boolean isSlient;
                if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                    isSlient = true;
                } else {
                    isSlient = false;

                    if (!user.equals(sender)) {
                        WFCMessage.Conversation conversation;
                        if (conversationType == ProtoConstants.ConversationType.ConversationType_Private) {
                            conversation = WFCMessage.Conversation.newBuilder().setType(conversationType).setLine(line).setTarget(sender).build();
                        } else {
                            conversation = WFCMessage.Conversation.newBuilder().setType(conversationType).setLine(line).setTarget(target).build();
                        }


                        if (m_messagesStore.getUserConversationSlient(user, conversation)) {
                            LOG.info("The conversation {}-{}-{} is slient", conversation.getType(), conversation.getTarget(), conversation.getLine());
                            isSlient = true;
                        }

                        if (m_messagesStore.getUserGlobalSlient(user)) {
                            LOG.info("??????{} ???????????????", user);
                            isSlient = true;
                        }
                    }

                    if (!StringUtil.isNullOrEmpty(pushContent) || messageContentType == 400 || messageContentType == 402) {
                        if (!isSlient) {
                            targetSession.setUnReceivedMsgs(targetSession.getUnReceivedMsgs() + 1);
                        }
                    }

                    if (isSlient) {
                        if (mentionType == 2 || (mentionType == 1 && mentionTargets.contains(user))) {
                            isSlient = false;
                        }
                    }
                }

                boolean needPush = !user.equals(sender);

                boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                boolean targetIsActiveToCluster = this.connectionDescriptors.isConnectedCluster(targetSession.getClientSession().clientID);

                if (targetIsActive) {
                    MqttPublishMessage publishMsg = getMqttPublishMessage(pullType, messageSeq, targetSession);
                    boolean sent = this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                    if (sent) {
                        needPush = false;
                    }
                } else if (targetIsActiveToCluster) {
                    //?????????????????????
                    MqttPublishMessage publishMsg = getMqttPublishMessage(pullType, messageSeq, targetSession);
                    LOG.info("??????????????????:{}  ??????????????????:{}   ???????????????????????????????????????:{}", IMTopic.NotifyMessageTopic, targetSession.getClientID(), targetIsActiveToCluster);
                    m_interceptor.notifyTopicPublished(publishMsg, targetSession.getClientID(), targetSession.getUsername());
                    needPush = false;
                } else {
                    LOG.info("??????{} ??????{}?????????", targetSession.getClientID(), targetSession.getUsername());
                }

                //TODO: ???????????????:  1.???????????????????????????, ??????????????????(????????????,????????????)  2.????????????????????????,??????????????????(????????????,????????????)
                if ((needPush && pullType != ProtoConstants.PullType.Pull_ChatRoom)) {
                    int curMentionType = 0;
                    if (mentionType == 2) {
                        curMentionType = 2;
                        isSlient = false;
                    } else if (mentionType == 1) {
                        if (mentionTargets != null && mentionTargets.contains(user)) {
                            curMentionType = 1;
                            isSlient = false;
                        }
                    }

                    if ((StringUtil.isNullOrEmpty(pushContent) && messageContentType != 402 && messageContentType != 400)) {
                        LOG.info("???????????????????????????contentType???{}", messageContentType);
                        continue;
                    }

                    if (isSlient) {
                        LOG.info("??????????????????????????????");
                        continue;
                    }

                    boolean isHiddenDetail = m_messagesStore.getUserPushHiddenDetail(user);

                    if (!nameLoaded) {
                        senderName = getUserDisplayName(sender, conversationType == ProtoConstants.ConversationType.ConversationType_Group ? target : null);
                        targetName = getTargetName(target, conversationType);
                        nameLoaded = true;
                    }

                    String name = senderName;
                    if (!sender.equals(user)) {
                        FriendData fd = m_messagesStore.getFriendData(user, sender);
                        if (fd != null && !StringUtil.isNullOrEmpty(fd.getAlias())) {
                            name = fd.getAlias();
                        }
                    }
                    /**
                     * TODO ?????????????????????????????????PUSH??????
                     *        ???????????????:  ?????? ??????????????????????????????????????????
                     *        ???????????????:  ?????? ???????????????????????????????????????
                     */
                    boolean connectedClusterFlag = connectionDescriptors.isConnectedCluster(targetSession.getClientID());

                    //??????????????????????????????
                    if (!connectedClusterFlag) {
                        this.messageSender.sendPush(sender, conversationType, target, line, messageHead, targetSession.getClientID(), pushContent, messageContentType, serverTime, name, targetName, targetSession.getUnReceivedMsgs(), curMentionType, isHiddenDetail, targetSession.getLanguage());
                    }
                }

            }
        }
    }

    private MqttPublishMessage getMqttPublishMessage(int pullType, long messageSeq, Session targetSession) {
        WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage
                .newBuilder()
                .setType(pullType)
                .setHead(messageSeq)
                .build();

        /**
         * TODO ?????? 1.PB?????? 2.??????mqtt?????? 3.??????MQTT
         */

        ByteBuf payload = Unpooled.buffer();
        LOG.info("MP ????????????:{}  ?????????:{}", ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage), targetSession.getClientID());
        byte[] byteData = ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage).getBytes();
        payload.ensureWritable(byteData.length).writeBytes(byteData);
        MqttPublishMessage publishMsg;
        publishMsg = notRetainedPublish(IMTopic.NotifyMessageTopic, MqttQoS.AT_MOST_ONCE, payload);
        return publishMsg;
    }

    private void publishTransparentMessage2Receivers(long messageHead, Collection<String> receivers, int pullType) {
        WFCMessage.Message message = m_messagesStore.getMessage(messageHead);

        if (message != null) {
            for (String user : receivers) {
                Collection<Session> sessions = m_sessionsStore.sessionForUser(user);

                for (Session targetSession : sessions) {
                    if (System.currentTimeMillis() - targetSession.getLastActiveTime() > 60 * 60 * 1000) {
                        continue;
                    }

                    if (targetSession.getClientID() == null) {
                        continue;
                    }

                    boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                    if (targetIsActive) {
                        ByteBuf payload = Unpooled.buffer();
                        byte[] byteData = message.toByteArray();
                        payload.ensureWritable(byteData.length).writeBytes(byteData);
                        MqttPublishMessage publishMsg;
                        publishMsg = notRetainedPublish(IMTopic.SendMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                        this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                    } else {
                        LOG.info("??????clientid{} ?????????{} ?????????", targetSession.getClientID(), targetSession.getUsername());
                    }
                }
            }
        }
    }

    public void publish2ReceiversNew(WFCMessage.Message message, Collection<String> receivers, int pullType) {
        if (message != null) {
            for (String user : receivers) {
                Collection<Session> sessions = m_sessionsStore.sessionForUser(user);

                for (Session targetSession : sessions) {
                    if (System.currentTimeMillis() - targetSession.getLastActiveTime() > 60 * 60 * 1000) {
                        continue;
                    }

                    if (targetSession.getClientID() == null) {
                        continue;
                    }

                    boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                    if (targetIsActive) {
                        WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage
                                .newBuilder()
                                .setType(pullType)
                                .setHead(m_messagesStore.getMessageHead(user))
                                .build();

                        ByteBuf payload = Unpooled.buffer();
                        //??????pb???json
//                    byte[] byteData = notifyMessage.toByteArray();
                        System.out.println("??????MS???????????????MP??????========" + ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage));
                        LOG.info("??????MS???????????????MP??????========" + ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage));
                        byte[] byteData = ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage).getBytes();
                        payload.ensureWritable(byteData.length).writeBytes(byteData);
                        MqttPublishMessage publishMsg;
                        publishMsg = notRetainedPublish(IMTopic.NotifyMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                        messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                    } else {
                        LOG.info("the target {} of user {} is not active", targetSession.getClientID(), targetSession.getUsername());
                    }
                }
            }
        }
    }

    public void publish2ReceiversNew(String userName, int pullType) {
        Collection<Session> sessions = m_sessionsStore.sessionForUser(userName);

        for (Session targetSession : sessions) {
            if (System.currentTimeMillis() - targetSession.getLastActiveTime() > 60 * 60 * 1000) {
                continue;
            }

            if (targetSession.getClientID() == null) {
                continue;
            }

            boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
            if (targetIsActive) {
                WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage
                        .newBuilder()
                        .setType(pullType)
                        .setHead(m_messagesStore.getMessageHead(userName))
                        .build();

                ByteBuf payload = Unpooled.buffer();
                LOG.info("??????????????????MP????????????========" + ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage) + "  ??????:" + targetSession.getClientSession().clientID);
                byte[] byteData = ProtoUtil.toJson(WFCMessage.NotifyMessage.class, notifyMessage).getBytes();
                payload.ensureWritable(byteData.length).writeBytes(byteData);
                MqttPublishMessage publishMsg;
                publishMsg = notRetainedPublish(IMTopic.NotifyMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
            } else {
                LOG.info("the target {} of user {} is not active", targetSession.getClientID(), targetSession.getUsername());
            }
        }
    }

    String getUserDisplayName(String userId, String groupId) {
        WFCMessage.User user = m_messagesStore.getUserInfo(userId);
        String userName = null;
        if (user != null) {
            userName = user.getDisplayName();
        }
        if (!StringUtil.isNullOrEmpty(groupId)) {
            WFCMessage.GroupMember member = m_messagesStore.getGroupMember(groupId, userId);
            if (member != null && !StringUtil.isNullOrEmpty(member.getAlias())) {
                userName = member.getAlias();
            }
        }
        return userName;
    }

    String getTargetName(String targetId, int cnvType) {
        if (cnvType == ProtoConstants.ConversationType.ConversationType_Private) {
            return getUserDisplayName(targetId, null);
        } else if (cnvType == ProtoConstants.ConversationType.ConversationType_Group) {
            WFCMessage.GroupInfo group = m_messagesStore.getGroupInfo(targetId);
            if (group != null) {
                return group.getName();
            }
        } else if (cnvType == ProtoConstants.ConversationType.ConversationType_Channel) {
            WFCMessage.ChannelInfo channelInfo = m_messagesStore.getChannelInfo(targetId);
            if (channelInfo != null) {
                return channelInfo.getName();
            }
        }
        return null;
    }

    public void publishNotification(String topic, String receiver, long head) {
        publishNotification(topic, receiver, head, null, null);
    }

    public void publishNotification(String topic, String receiver, long head, String fromUser, String pushContent) {
        publishNotificationLocal(topic, receiver, head, fromUser, pushContent);
    }

    void publishNotificationLocal(String topic, String receiver, long head, String fromUser, String pushContent) {
        Collection<Session> sessions = m_sessionsStore.sessionForUser(receiver);
        String fromUserName = null;
        for (Session targetSession : sessions) {
            boolean needPush = !StringUtil.isNullOrEmpty(pushContent);
            boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
            int clusterNodeByClientId = this.connectionDescriptors.getClusterNodeByClientId(targetSession.getClientSession().clientID);
            LOG.info("????????????:{}  ????????????:{}   ????????????????????????:{}   ????????????????????????{}", topic, targetSession.getClientID(), targetIsActive, clusterNodeByClientId);
            if (targetIsActive) {
                ByteBuf payload = Unpooled.buffer();
                payload.writeLong(head);
                MqttPublishMessage publishMsg;
                publishMsg = notRetainedPublish(topic, MqttQoS.AT_MOST_ONCE, payload);

                boolean result = this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                if (!result) {
                    LOG.warn("Publish friend request failure");
                } else {
                    needPush = false;
                }
            }
            String nodeId = Server.getServer().getConfig().getProperty(NODE_ID);
            boolean needPushToCluster = !targetIsActive && (NON_EXIST_NODE != clusterNodeByClientId) && (Integer.parseInt(nodeId) != clusterNodeByClientId);
            //??????????????? ?????????????????????????????????  ??????????????????
            if (needPushToCluster) {
                LOG.info("??????????????????:{}  ??????????????????:{}   ???????????????????????????????????????:{}", topic, targetSession.getClientID(), clusterNodeByClientId);
                ByteBuf payload = Unpooled.buffer();
                payload.writeLong(head);
                MqttPublishMessage publishMsg;
                publishMsg = notRetainedPublish(topic, MqttQoS.AT_MOST_ONCE, payload);
                m_interceptor.notifyTopicPublished(publishMsg, targetSession.getClientID(), receiver);
                needPush = false;
            }
            if (needPush) {
                if (fromUserName == null) {
                    WFCMessage.User userInfo = m_messagesStore.getUserInfo(fromUser);
                    if (userInfo == null) {
                        fromUserName = "";
                    } else {
                        fromUserName = userInfo.getDisplayName();
                    }
                }

                if (IMTopic.NotifyFriendRequestTopic.equals(topic)) {
                    messageSender.sendPush(fromUser, receiver, targetSession.getClientID(), pushContent, PushServer.PushMessageType.PUSH_MESSAGE_TYPE_FRIEND_REQUEST, System.currentTimeMillis(), fromUserName, targetSession.getUnReceivedMsgs() + 1, targetSession.getLanguage());
                }
            }
        }
    }

    public void publishNotificationLocalClientId(String topic, String clientId, long head) {
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientId);
        boolean targetIsActive = this.connectionDescriptors.isConnected(clientId);
        int clusterNodeByClientId = this.connectionDescriptors.getClusterNodeByClientId(clientId);
        LOG.info("????????????:{}  ????????????:{}   ????????????????????????:{}   ????????????????????????{}", topic, clientId, targetIsActive, clusterNodeByClientId);
        if (targetIsActive) {
            ByteBuf payload = Unpooled.buffer();
            payload.writeLong(head);
            MqttPublishMessage publishMsg;
            publishMsg = notRetainedPublish(topic, MqttQoS.AT_MOST_ONCE, payload);

            boolean result = this.messageSender.sendPublish(targetSession, publishMsg);
            if (!result) {
                LOG.warn("Publish friend request failure");
            }
        } else {
            LOG.info("????????????????????????:{}  ??????:{}  ????????? ?????????????????????????????????   ?????????:{}",clientId,topic,clusterNodeByClientId);
        }
    }

    public void updateChatroomMembersQueue(String chatroomId, int line, long messageId) {
        final long messageSeq = m_messagesStore.insertChatroomMessages(chatroomId, line, messageId);
        Collection<UserClientEntry> members = m_messagesStore.getChatroomMembers(chatroomId);
        for (UserClientEntry member : members
        ) {
            chatRoomHeaders.compute(member, new BiFunction<UserClientEntry, Long, Long>() {
                @Override
                public Long apply(UserClientEntry s, Long aLong) {
                    if (aLong == null)
                        return messageSeq;
                    if (messageSeq > aLong)
                        return messageSeq;
                    return aLong;
                }
            });
        }
    }

    public void publishRecall2ReceiversLocal(long messageUid, String operatorId, Collection<String> receivers, String exceptClientId) {
        for (String user : receivers) {


            Collection<Session> sessions = m_sessionsStore.sessionForUser(user);
            for (Session targetSession : sessions) {
                if (exceptClientId != null && exceptClientId.equals(targetSession.getClientSession().clientID)) {
                    continue;
                }

                if (targetSession.getClientID() == null) {
                    continue;
                }

                boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                if (targetIsActive) {
                    WFCMessage.NotifyRecallMessage notifyMessage = WFCMessage.NotifyRecallMessage
                            .newBuilder()
                            .setFromUser(operatorId)
                            .setId(messageUid)
                            .build();

                    ByteBuf payload = Unpooled.buffer();
                    byte[] byteData = notifyMessage.toByteArray();
                    payload.ensureWritable(byteData.length).writeBytes(byteData);
                    MqttPublishMessage publishMsg;
                    publishMsg = notRetainedPublish(IMTopic.NotifyRecallMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                    this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                } else {
                    LOG.info("the target {} of user {} is not active", targetSession.getClientID(), targetSession.getUsername());
                }
            }
        }
    }

    public void publishRecall2Receivers(long messageUid, String operatorId, Set<String> receivers, String exceptClientId) {
        publishRecall2ReceiversLocal(messageUid, operatorId, receivers, exceptClientId);
    }

    //TODO ????????????
    public void publish2Receivers(WFCMessage.Message message, Set<String> receivers, String exceptClientId, int pullType) {
        executeChannel(message);
        long messageId = message.getMessageId();

        String pushContent = message.getContent().getPushContent();
        if (StringUtil.isNullOrEmpty(pushContent)) {
            int type = message.getContent().getType();
            if (type == ProtoConstants.ContentType.Image) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.Location) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.Text) {
                pushContent = message.getContent().getSearchableContent();
            } else if (type == ProtoConstants.ContentType.Voice) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.Video) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.RichMedia) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.File) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.Sticker) {
                pushContent = "[??????]";
            } else if (type == ProtoConstants.ContentType.Comments) {
                pushContent = "[??????]";
            }
        }

        if (message.getContent().getPersistFlag() == Transparent) {
            pushContent = null;
        }

        publish2Receivers(message.getFromUser(),
                message.getConversation().getType(), message.getConversation().getTarget(), message.getConversation().getLine(),
                messageId,
                receivers,
                pushContent, exceptClientId, pullType, message.getContent().getType(), message.getServerTimestamp(), message.getContent().getMentionedType(), message.getContent().getMentionedTargetList(), message.getContent().getPersistFlag());

    }

    //TODO ??????????????????????????????????????????????????????
    public void publish2ReceiversNew(WFCMessage.Message message, Set<String> receivers, String exceptClientId, int pullType) {
        executeChannel(message);
        publish2ReceiversNew(message, receivers, pullType);
    }

    private void executeChannel(WFCMessage.Message message) {
        if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel) {
            WFCMessage.ChannelInfo channelInfo = m_messagesStore.getChannelInfo(message.getConversation().getTarget());
            if (channelInfo != null && !StringUtil.isNullOrEmpty(channelInfo.getCallback())) {
                executorCallback.execute(() -> HttpUtils.httpJsonPost(channelInfo.getCallback() + "/message", new Gson().toJson(SendMessageData.fromProtoMessage(message), SendMessageData.class)));
            }
        }
    }

    public void forwardMessage(final WFCMessage.Message message, String forwardUrl) {
        executorCallback.execute(() -> HttpUtils.httpJsonPost(forwardUrl, new Gson().toJson(OutputMessageData.fromProtoMessage(message), OutputMessageData.class)));
    }

    public void notifyChannelListenStatusChanged(WFCMessage.ChannelInfo channelInfo, String user, boolean listen) {
        if (channelInfo == null || StringUtil.isNullOrEmpty(channelInfo.getCallback())) {
            return;
        }
        executorCallback.execute(() -> HttpUtils.httpJsonPost(channelInfo.getCallback() + "/subscribe", new Gson().toJson(new OutputNotifyChannelSubscribeStatus(user, channelInfo.getTargetId(), listen), OutputNotifyChannelSubscribeStatus.class)));
    }
}

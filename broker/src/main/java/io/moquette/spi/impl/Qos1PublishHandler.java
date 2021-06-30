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
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.OutputCheckUserOnline;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.google.gson.Gson;
import com.secret.loServer.action.ClassUtil;
import io.moquette.BrokerConstants;
import io.moquette.imhandler.FilterTopicHandler;
import io.moquette.imhandler.Handler;
import io.moquette.imhandler.IMHandler;
import io.moquette.liuyan.utils.LaoLiuUtils;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.persistence.RPCCenter;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.AES;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.IMTopic;
import com.liuyan.im.MessageShardingUtil;
import com.liuyan.im.RateLimiter;
import com.liuyan.im.Utility;
import com.liuyan.im.extended.mqttmessage.ModifiedMqttPubAckMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import static cn.wildfirechat.common.ErrorCode.*;
import static io.moquette.liuyan.contants.LaoLiuConstants.HazelcastConstants.ZERO;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

public class Qos1PublishHandler extends QosPublishHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final IMessagesStore m_messagesStore;
    private final ConnectionDescriptorStore connectionDescriptors;
    private final MessagesPublisher publisher;
    private final ISessionsStore m_sessionStore;
    private final ThreadPoolExecutorWrapper m_imBusinessExecutor;
    private final RateLimiter mLimitCounter = new RateLimiter(5, 100);

    private HashMap<String, IMHandler> m_imHandlers = new HashMap<>();
    private Set<String> filterTopics = new HashSet<>();
    private final BrokerInterceptor m_interceptor;

    public Qos1PublishHandler(IAuthorizator authorizator, IMessagesStore messagesStore, BrokerInterceptor interceptor,
                              ConnectionDescriptorStore connectionDescriptors, MessagesPublisher messagesPublisher,
                              ISessionsStore sessionStore, ThreadPoolExecutorWrapper executorService, Server server) {
        super(authorizator);
        this.m_messagesStore = messagesStore;
        this.connectionDescriptors = connectionDescriptors;
        this.publisher = messagesPublisher;
        this.m_sessionStore = sessionStore;
        this.m_imBusinessExecutor = executorService;
        this.m_interceptor = interceptor;
        IMHandler.init(m_messagesStore, m_sessionStore, publisher, m_imBusinessExecutor, server);
        registerAllAction();
    }

    public HashMap<String, IMHandler> getImHandlers() {
        return m_imHandlers;
    }

    private void registerAllAction() {
        try {
            for (Class cls : ClassUtil.getAllAssignedClass(IMHandler.class)) {
                Handler annotation = (Handler) cls.getAnnotation(Handler.class);
                if (annotation != null) {
                    IMHandler handler = (IMHandler) com.xiaoleilu.hutool.util.ClassUtil.newInstance(cls);
                    m_imHandlers.put(annotation.value(), handler);
                    //TODO:初始化时增加的，可以通过注解去指定
                    if (Objects.nonNull(cls.getAnnotation(FilterTopicHandler.class))) {
                        filterTopics.add(annotation.value());
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    /**
     * @param fromUser
     * @param clientId
     * @param message
     * @param requestId
     * @param from
     * @param request
     * @param isAdmin
     */
    public void onRpcMsg(String fromUser, String clientId, byte[] message, int requestId, String from, String request, boolean isAdmin) {
        if (request.equals(RPCCenter.CHECK_USER_ONLINE_REQUEST)) {
            //TODO 在线返回响应
            checkUserOnlineHandler(message, ackPayload -> RPCCenter.getInstance().sendResponse(ERROR_CODE_SUCCESS.getCode(), ackPayload, from, requestId));
        } else {
            //TODO 离线返回响应
            imHandler(clientId, fromUser, request, message, (errorCode, ackPayload) -> {
                if (requestId > 0) {
                    byte[] response = new byte[ackPayload.readableBytes()];
                    ackPayload.readBytes(response);
                    ReferenceCountUtil.release(ackPayload);
                    RPCCenter.getInstance().sendResponse(errorCode.getCode(), response, from, requestId);
                }
            }, isAdmin);
        }
    }


    void checkUserOnlineHandler(byte[] payloadContent, RouteCallback callback) {
        m_imBusinessExecutor.execute(() -> {
            String userId = new String(payloadContent);

            int status;
            Collection<MemorySessionStore.Session> useSessions = m_sessionStore.sessionForUser(userId);
            OutputCheckUserOnline out = new OutputCheckUserOnline();

            for (MemorySessionStore.Session session : useSessions) {
                if (session.getDeleted() > 0) {
                    continue;
                }
                String clientID = session.getClientID();
                ConnectionDescriptor descriptor = connectionDescriptors.getConnection(clientID);
                if (descriptor == null && !connectionDescriptors.isConnectedCluster(clientID)) {
                    status = 1;
                } else {
                    status = 0;
                }

                out.addSession(userId, session.getClientID(), session.getPlatform(), status, session.getLastActiveTime());
            }

            callback.onRouteHandled(new Gson().toJson(out).getBytes());
        });
    }

    void imHandler(String clientID, String fromUser, String topic, byte[] payloadContent, IMCallback callback, boolean isAdmin) {
        LOG.info("imHandler fromUser={}, topic={}", fromUser, topic);
        IMCallback wrapper = getHandlerCallbackWrapper(clientID, fromUser, topic, callback);
        if (wrapper == null) return;

        IMHandler handler = m_imHandlers.get(topic);
        if (handler != null) {
            handler.doHandler(clientID, fromUser, topic, payloadContent, wrapper, isAdmin);
        } else {
            ifUnknownTopic(topic, wrapper);
        }
    }

    void clusterHandlerPubAck(String clientID, String fromUser, String topic, byte[] payloadContent, IMCallback callback, boolean isAdmin) {
        LOG.info("imClusterPubAck fromUser={}, topic={}", fromUser, topic);
        IMCallback wrapper = getHandlerCallbackWrapper(clientID, fromUser, topic, callback);
        if (wrapper == null) return;
        String clusterTopic = topic;
        if (IMTopic.SendMessageTopic.equalsIgnoreCase(topic)) {
            clusterTopic = IMTopic.SendMessageToClusterTopic;
        } else if (IMTopic.NewAddFriendTopic.equalsIgnoreCase(topic)) {
            clusterTopic = IMTopic.NewAddFriendToClusterTopic;
        }

        IMHandler handler = m_imHandlers.get(clusterTopic);
        if (handler != null) {
            //只发送ack  不处理
            handler.doClsterHandler(clientID, fromUser, topic, payloadContent, wrapper, isAdmin);
        } else {
            ifUnknownTopic(topic, wrapper);
        }
    }

    private boolean limitFromUserRequest(String clientID, String fromUser, String topic, IMCallback callback) {
        if (!mLimitCounter.isGranted(clientID + fromUser + topic)) {
            ByteBuf ackPayload = Unpooled.buffer();
            ackPayload.ensureWritable(1).writeByte(ERROR_CODE_OVER_FREQUENCY.getCode());
            try {
                callback.onIMHandled(ERROR_CODE_OVER_FREQUENCY, ackPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
            LOG.warn("用户 {} 的请求频率过高", fromUser);
            return true;
        }
        return false;
    }

    private IMCallback getHandlerCallbackWrapper(String clientID, String fromUser, String topic, IMCallback callback) {
        if (limitFromUserRequest(clientID, fromUser, topic, callback)) return null;

        IMCallback wrapper = (errorcode, ackPayload) -> {
            ackPayload.resetReaderIndex();
            byte code = ackPayload.readByte();
            if (ackPayload.readableBytes() > 0) {
                byte[] data = new byte[ackPayload.readableBytes()];
                ackPayload.getBytes(1, data);
                try {
                    //clientID 为空的是server api请求。客户端不允许clientID为空
                    if (!StringUtil.isNullOrEmpty(clientID)) {
                        //在route时，使用系统根密钥。当route成功后，用户都使用用户密钥
                        if (topic.equals(IMTopic.GetTokenTopic)) {
                            data = AES.AESEncrypt(data, "");
                        } else {
                            MemorySessionStore.Session session = m_sessionStore.getSession(clientID);
                            if (session != null && session.getUsername().equals(fromUser)) {
                                if (data.length > 7 * 1024 && session.getMqttVersion().protocolLevel() >= MqttVersion.Wildfire_1.protocolLevel()) {
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    GZIPOutputStream gzip;
                                    try {
                                        gzip = new GZIPOutputStream(out);
                                        gzip.write(data);
                                        gzip.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Utility.printExecption(LOG, e);
                                    }
                                    data = out.toByteArray();
                                    code = (byte) ErrorCode.ERROR_CODE_SUCCESS_GZIPED.code;
                                }
                            }
                        }
                    }
                    ackPayload.clear();
                    ackPayload.resetWriterIndex();
                    ackPayload.writeByte(code);
                    ackPayload.writeBytes(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }
            }
            ackPayload.resetReaderIndex();
            try {
                callback.onIMHandled(errorcode, ackPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        };
        return wrapper;
    }

    private void ifUnknownTopic(String topic, IMCallback wrapper) {
        LOG.error("imHandler unknown topic={}", topic);
        ByteBuf ackPayload = Unpooled.buffer();
        ackPayload.ensureWritable(1).writeByte(ERROR_CODE_NOT_IMPLEMENT.getCode());
        try {
            wrapper.onIMHandled(ERROR_CODE_NOT_IMPLEMENT, ackPayload);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    public interface IMCallback {
        void onIMHandled(ErrorCode errorCode, ByteBuf ackPayload);
    }

    interface RouteCallback {
        void onRouteHandled(byte[] ackPayload);
    }

    /**
     * 1. 两台分开  一台连接,另一台连接    期望 不推送 调用集群通讯
     * 2. 两台分开  一台连接,另一台不连接  期望 推送   不调用集群通讯
     * 3. 两台一起  一台连接,另一台连接    期望 不推送 不调用集群通讯
     * 4. 两台一起  一台连接,另一台不连接  期望 推送   不调用集群通讯
     *
     * @param channel
     * @param msg
     */
    void receivedPublishQos1(Channel channel, MqttPublishMessage msg) {
        // 验证是否可以写入对应主题
        final Topic topic = new Topic(msg.variableHeader().topicName());

        //TODO:此处获取的ClientId是发送者的Id和userName
        String sendClientId = NettyUtils.clientID(channel);
        String sendUserName = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, sendUserName, sendClientId)) {
            LOG.error("MQTT客户端没有权限在该主题发布消息。 CId={}, topic={}", sendClientId, topic);
            return;
        }

        final int messageID = msg.variableHeader().packetId();
        String imTopic = topic.getTopic();
        ByteBuf payload = msg.payload();
        byte[] payloadContent = readBytesAndRewind(payload);

        //TODO:此处获取的发送者的session
        MemorySessionStore.Session sendSession = m_sessionStore.getSession(sendClientId);
        String targetUserNameOne = null;
        /**
         * TODO 2020年12月17日17点43分
         *        直接通知不成功时，发送PUSH消息
         *        处理消息处:  或者 这个人没连任何机器的时候处理
         *        推送消息处:  并且 这个人连某台机器的时候推送
         *  目前缺陷: 直接全推,现在只有两台 所以还好说  以后有多台  通过NodeId去推送,在接受消息处(onMessage)进行处理
         */
        if (IMTopic.SendMessageTopic.equalsIgnoreCase(imTopic)) {
            Set<String> notifyReceivers = new LinkedHashSet<>();
            IMHandler<WFCMessage.Message> imHandler = m_imHandlers.get(IMTopic.SendMessageTopic);
            WFCMessage.Message message = null;
            try {
                message = imHandler.getDataObject(payloadContent);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                LaoLiuUtils.errorLog("receivedPublishQos1转换payloadContent发生异常:{}", e);
            }
            WFCMessage.Message.Builder messageBuilder = message.toBuilder();
            int pullType = m_messagesStore.getNotifyReceivers(sendUserName, messageBuilder, notifyReceivers, false);
            //TODO:2020年12月21日16点26分 对应的消息messageId存储对应的接受者
            ConcurrentHashMap<String, String> targetUserNameMap = new ConcurrentHashMap<>();
            for (String targetUserName : notifyReceivers) {
                //此处的userName是发送者的
                if (!StringUtil.isNullOrEmpty(sendUserName) && sendUserName.equals(targetUserName)) {
                    continue;
                }
                targetUserNameMap.put(sendClientId, targetUserName);
            }
            targetUserNameOne = targetUserNameMap.get(sendClientId);


            //需要在转发前同步告诉前端 而非异步   是否审核成功  消息id  所以业务层面提前到集群转发层面
            long timestamp = System.currentTimeMillis();
            long messageId = MessageShardingUtil.generateId();
            message = message.toBuilder().setFromUser(sendUserName).setMessageId(messageId).setServerTimestamp(timestamp).build();
            //该消息可转换成map  专门给前端返回
            String sendToClusterMsg = ProtoUtil.toJson(WFCMessage.Message.class, message);

            //将带有消息id和时间戳的消息推送出去,不在业务层面生成了
            ByteBuf toClusterByteBuffer = Unpooled.wrappedBuffer(sendToClusterMsg.getBytes());
            payloadContent = readBytesAndRewind(toClusterByteBuffer);
            msg = msg.replace(toClusterByteBuffer);
        } else if (IMTopic.NewAddFriendTopic.equalsIgnoreCase(imTopic)) {
            IMHandler<WFCMessage.PullUserRequest> imHandler = m_imHandlers.get(IMTopic.NewAddFriendTopic);
            WFCMessage.PullUserRequest userRequestList = null;
            try {
                userRequestList = imHandler.getDataObject(payloadContent);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                LaoLiuUtils.errorLog("receivedPublishQos1转换payloadContent发生异常:{}", e);
            }
            List<WFCMessage.UserRequest> requestList = userRequestList.getRequestList();
            for (WFCMessage.UserRequest userRequest : requestList) {
                // 拿出所有对方的userId, 然后拼接  一对一调用添加好友接口
                WFCMessage.PullUserRequest toImHandlerPullUserRequest = WFCMessage.PullUserRequest.newBuilder()
                        .addRequest(userRequest)
                        .build();

                //将请求实体类转湖json   json的byte数组转为buffer   buffer转字节数组   字节数组用于payload   buffer用于替换之前的前端buffer为我们自定义buffer
                ByteBuf byteBuf = Unpooled.wrappedBuffer(ProtoUtil.toJson(WFCMessage.PullUserRequest.class, toImHandlerPullUserRequest)
                        .getBytes());
                byte[] imInnerPayloadContent = readBytesAndRewind(byteBuf);
                MqttPublishMessage replaceClusterMsg = msg.replace(byteBuf);
                sendMsgToCluster(replaceClusterMsg, topic, sendClientId, sendUserName, messageID, imTopic, imInnerPayloadContent, userRequest.getUid());
            }
            return;
        }
        sendMsgToCluster(msg, topic, sendClientId, sendUserName, messageID, imTopic, payloadContent, targetUserNameOne);
    }

    private void sendMsgToCluster(MqttPublishMessage msg, Topic topic, String sendClientId, String sendUserName, int messageID, String imTopic, byte[] payloadContent, String targetUserNameOne) {
        //不存在该用户的情况/该用户未连接的情况,需要进行处理,但是不用集群之间通讯
        if (StringUtil.isNullOrEmpty(targetUserNameOne)) {
            imHandler(sendClientId, sendUserName, imTopic, payloadContent, (errorCode, ackPayload) -> sendPubAck(sendClientId, messageID, ackPayload, errorCode), false);
            return;
        }

        //获取接受者的session连接信息
        Collection<MemorySessionStore.Session> sessions = m_sessionStore.sessionForUser(targetUserNameOne);

        //没有该用户的session信息
        if (LaoLiuUtils.isEmpty(sessions)) {
            LOG.error(targetUserNameOne + "===========不存在接受消息的用户(消费方客户端)的session数据!");
            imHandler(sendClientId, sendUserName, imTopic, payloadContent, (errorCode, ackPayload) -> sendPubAck(sendClientId, messageID, ackPayload, errorCode), false);
            return;
        }

        ArrayList<MemorySessionStore.Session> sessionArrayList = new ArrayList<>(sessions);
        MemorySessionStore.Session targetSession = sessionArrayList.get(ZERO);
        //只处理最后一次登录的session  最近的session
        for (MemorySessionStore.Session session : sessions) {
            if (session.getLastActiveTime() > targetSession.getLastActiveTime()) {
                targetSession = session;
            }
        }

        String targetClientID = targetSession.getClientID();

        //目标所在节点
        int targetNodeId = connectionDescriptors.getClusterNodeByClientId(targetClientID);

        //本机节点
        int thisNodeId = Integer.parseInt(Server.getServer().getConfig().getProperty(BrokerConstants.NODE_ID));

        //目标是否存活
        boolean connectedClusterFlag = connectionDescriptors.isConnectedCluster(targetClientID);

        //目标不在本机的时候  并且存活的时候 处理  而且推送出去  推送出去那台发现是自己 就不处理了
        boolean pushClusterFlag = connectedClusterFlag && (targetNodeId != thisNodeId);

        if (!pushClusterFlag) {
            //TODO 2.根据Topic选择相应的Handler
            imHandler(sendClientId, sendUserName, imTopic, payloadContent, (errorCode, ackPayload) -> sendPubAck(sendClientId, messageID, ackPayload, errorCode), false);
        }

        //TODO: 新增的  比如添加好友，只是持久化一下 不需要发送消息，本质就是一个过滤器，过滤哪些广播那些不广播,过滤掉不需要广播的消息，防止某些持久化行为 操作多次
        if (!filterTopics.contains(imTopic) && pushClusterFlag) {
            //TODO： 需要进行广播的
            notifyTopicPublish(msg, topic, sendClientId, sendUserName);
            clusterHandlerPubAck(sendClientId, sendUserName, imTopic, payloadContent, (errorCode, ackPayload) -> sendPubAck(sendClientId, messageID, ackPayload, errorCode), false);
        }
    }

    private void notifyTopicPublish(MqttPublishMessage msg, Topic topic, String clientID, String username) {
        //TODO: 转化消息 创建一个IMessagesStore.StoredMessage，同时把消息推送给所有该对该消息的订阅者。
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        if (msg.fixedHeader().isRetain()) {
            if (!msg.payload().isReadable()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                // 之前没有储存
                m_messagesStore.storeRetained(topic, toStoreMsg);
            }
        }
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    public void sendPubAck(String clientId, int messageID, ByteBuf payload, ErrorCode errorCode) {
        LOG.trace("sendPubAck被调用");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        ModifiedMqttPubAckMessage pubAckMessage = new ModifiedMqttPubAckMessage(fixedHeader, from(messageID), payload);

        try {
            if (connectionDescriptors == null) {
                throw new RuntimeException("内部错误，发现connectionDescriptors为空，而它应该被初始化，在某处它被覆盖!!");
            }
            LOG.debug("clientIDs are {}", connectionDescriptors);
            if (!connectionDescriptors.isConnected(clientId)) {
                throw new RuntimeException(String.format("在缓存%s中找不到客户端%s的连接描述符",
                        clientId, connectionDescriptors));
            }
            connectionDescriptors.sendMessage(pubAckMessage, messageID, clientId, errorCode);
        } catch (Throwable t) {
            LOG.error(null, t);
        }
    }

}

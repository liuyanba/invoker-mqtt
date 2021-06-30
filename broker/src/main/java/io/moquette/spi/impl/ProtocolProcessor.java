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

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.UserOnlineStatus;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.hazelcast.core.IMap;
import com.hazelcast.util.StringUtil;
import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.HttpUtils;
import com.liuyan.im.Utility;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.moquette.server.ConnectionDescriptor.ConnectionState.*;
import static io.moquette.spi.impl.InternalRepublisher.createPublishForQos;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of the protocol
 * execution.
 * <p>
 * Used by the front facing class ProtocolProcessorBootstrapper.
 */

public class ProtocolProcessor {
    private static ExecutorService executorCallback = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void kickoffSession(final MemorySessionStore.Session session) {
        mServer.getImBusinessScheduler().execute(() -> {
            ConnectionDescriptor descriptor = connectionDescriptors.getConnection(session.getClientID());
            try {
                if (descriptor != null) {
                    processDisconnect(descriptor.getChannel(), true, false);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        });
    }

    private void handleTargetRemovedFromCurrentNode(TargetEntry target) {
        System.out.println("kickof user " + target);
        if (target.type == TargetEntry.Type.TARGET_TYPE_USER) {
            Collection<MemorySessionStore.Session> sessions = m_sessionsStore.sessionForUser(target.target);
            for (MemorySessionStore.Session session : sessions) {
                ConnectionDescriptor descriptor = connectionDescriptors.getConnection(session.getClientID());
                try {
                    if (descriptor != null) {
                        processDisconnect(descriptor.getChannel(), true, false);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }
            }
        } else if (target.type == TargetEntry.Type.TARGET_TYPE_CHATROOM) {

        }
    }


    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);

    protected ConnectionDescriptorStore connectionDescriptors;//所有的连接描述符文存储，即clientId与通道之间的映射集合

    private IAuthorizator m_authorizator;//对topic的读写权限认证

    private IMessagesStore m_messagesStore;//retainMessage的存储

    private ISessionsStore m_sessionsStore;//session 存储

    private IAuthenticator m_authenticator;//连接时候的鉴权认证
    private BrokerInterceptor m_interceptor;//TODO: 各个层面的拦截器

    public Qos1PublishHandler qos1PublishHandler;//qos1拦截器
    private MessagesPublisher messagesPublisher;//分发消息，遗愿消息，以及集权间同步消息

    private Server mServer;


    ProtocolProcessor() {

    }

    /**
     * @param storageService the persistent store to use for save/load of messages for QoS1 and QoS2 handling.
     * @param sessionsStore  the clients sessions store, used to persist subscriptions.
     * @param authenticator  true to allow clients connect without a clientid
     * @param authorizator   used to apply ACL policies to publishes and subscriptions.
     * @param interceptor    to notify events to an intercept handler
     */
    void init(ConnectionDescriptorStore connectionDescriptors,
              IMessagesStore storageService, ISessionsStore sessionsStore, IAuthenticator authenticator, IAuthorizator authorizator,
              BrokerInterceptor interceptor, Server server) {
        LOG.info("Initializing MQTT protocol processor...");
        this.connectionDescriptors = connectionDescriptors;
        this.m_interceptor = interceptor;
        m_authorizator = authorizator;
        m_authenticator = authenticator;
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;

        LOG.info("Initializing messages publisher...");
        final PersistentQueueMessageSender messageSender = new PersistentQueueMessageSender(this.connectionDescriptors);
        this.messagesPublisher = new MessagesPublisher(connectionDescriptors, sessionsStore, messageSender, server.getHazelcastInstance(), m_messagesStore, m_interceptor);

        LOG.info("Initializing QoS publish handlers...");
        this.qos1PublishHandler = new Qos1PublishHandler(m_authorizator, m_messagesStore, m_interceptor,
                this.connectionDescriptors, this.messagesPublisher, sessionsStore, server.getImBusinessScheduler(), server);

        mServer = server;

        String onlineStatusCallback = server.getConfig().getProperty(BrokerConstants.USER_ONLINE_STATUS_CALLBACK);
        if (!com.hazelcast.util.StringUtil.isNullOrEmpty(onlineStatusCallback)) {
            mUserOnlineStatusCallback = onlineStatusCallback;
        }
    }

    private String mUserOnlineStatusCallback;

    public void processConnect(Channel channel, MqttConnectMessage msg) {
        MqttConnectPayload payload = msg.payload();
        String clientId = payload.clientIdentifier();
        LOG.info("正在处理CONNECT消息。 CId={}, username={}", clientId, payload.userName());

        // 1. 判断客户端连接时发送的MQTT协议版本号，非3.1和3.1.1版本发送协议不支持响应报文并在发送完成后关闭连接
        if (msg.variableHeader().version() < MqttVersion.MQTT_3_1_1.protocolLevel() ||
                msg.variableHeader().version() >= MqttVersion.Wildfire_Max.protocolLevel()) {
            MqttConnAckMessage badProto = connAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);

            LOG.error("MQTT协议版本无效。 CId={}", clientId);
            channel.writeAndFlush(badProto);
            channel.close();
            return;
        }

        if (clientId == null || clientId.length() == 0) {
            // 2. 在客户端配置了cleanSession=false 或者服务端不允许clientId不存在的情况下客户端如果未上传clientId发送协议不支持响应报文并在发送完成后关闭连接
            MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);

            channel.writeAndFlush(badId);
            channel.close();
            LOG.error("MQTT客户端ID不能为空。 Username={}", payload.userName());
            return;
        }


        MqttVersion mqttVersion = MqttVersion.fromProtocolLevel(msg.variableHeader().version());
        // 3. 判断用户名和密码是否合法
        if (!login(channel, msg, clientId, mqttVersion)) {
            channel.flush();
            channel.close();
            LOG.error("MQTT登录失败。 Username={}", payload.userName());
            return;
        }
        if (!mServer.m_initialized) {
            channel.close();
            return;
        }

        // 4. 初始化连接对象并将连接对象引用放入连接管理中，如果发现连接管理中存在相同客户端ID的对象则关闭前一个连接并将新的连接对象放入连接管理中
        ConnectionDescriptor descriptor = new ConnectionDescriptor(clientId, channel);
        ConnectionDescriptor existing = this.connectionDescriptors.addConnection(descriptor);
        if (existing != null) {
            //TODO: 如果已经有该连接 删了重加一次
            LOG.info("现有连接中正在使用客户端ID。它将关闭。 CId={}", clientId);
            this.connectionDescriptors.removeConnection(existing);
            existing.abort();
            this.connectionDescriptors.addConnection(descriptor);
        }

        // 5. 根据客户端上传的心跳时间调整服务端当前连接的心跳判断时间（keepAlive * 1.5f）
        initializeKeepAliveTimeout(channel, msg, clientId);

        // 6. 发送连接成功响应
        if (!sendAck(descriptor, msg, clientId)) {
            channel.close();
            return;
        }

        m_interceptor.notifyClientConnected(msg);

        // 8. 创建当前连接session
        final ClientSession clientSession = createOrLoadClientSession(payload.userName(), descriptor, msg, clientId);

        // 9. 当cleanSession=false 发送当前session已经存储的消息
        if (clientSession == null) {
            MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_SESSION_NOT_EXIST);

            channel.writeAndFlush(badId);
            channel.close();
            return;
        }


        int flushIntervalMs = 500/* (keepAlive * 1000) / 2 */;
        descriptor.setupAutoFlusher(flushIntervalMs);

        final boolean success = descriptor.assignState(SESSION_CREATED, ESTABLISHED);
        if (!success) {
            channel.close();
            return;
        }
        MemorySessionStore.Session session = m_sessionsStore.getSession(clientId);
        if (session != null) {
            session.refreshLastActiveTime();
            forwardOnlineStatusEvent(payload.userName(), clientId, session.getPlatform(), UserOnlineStatus.ONLINE);
            m_messagesStore.updateUserOnlineSetting(session, true);
        } else {
            LOG.error("连接时未寻找到该client的session:{}", clientId);
            forwardOnlineStatusEvent(payload.userName(), clientId, ProtoConstants.Platform.Platform_UNSET, UserOnlineStatus.ONLINE);
        }

        LOG.info("CONNECT消息已处理。 clientId = {}, username={}", clientId, payload.userName());
    }

    public void forwardOnlineStatusEvent(String userId, String clientId, int platform, int status) {
        if (!StringUtil.isNullOrEmpty(mUserOnlineStatusCallback)) {
            executorCallback.execute(() -> HttpUtils.httpJsonPost(mUserOnlineStatusCallback, new Gson().toJson(new UserOnlineStatus(userId, clientId, platform, status))));
        }
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode) {
        return connAck(returnCode, false);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        return connAck(returnCode, sessionPresent, null);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, byte[] data) {
        return connAck(returnCode, false, data);
    }


    private MqttConnAckMessage connAckWithSessionPresent(MqttConnectReturnCode returnCode, byte[] data) {
        return connAck(returnCode, true, data);
    }


    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent, byte[] data) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader, new MqttConnectAckPayload(data));
    }

    //TODO 登录
    private boolean login(Channel channel, MqttConnectMessage msg, final String clientId, MqttVersion mqttVersion) {
        // handle user authentication
        if (msg.variableHeader().hasUserName()) {
            int status = m_messagesStore.getUserStatus(msg.payload().userName());
            if (status == ProtoConstants.UserStatus.Forbidden) {
                //通过流写出 2 标识已封禁
                failedBlocked(channel);
                return false;
            }
            byte[] pwd = null;
            if (msg.variableHeader().hasPassword()) {
                pwd = msg.payload().password();

                MemorySessionStore.Session session = m_sessionsStore.getSession(clientId);
                if (session == null) {
                    ErrorCode errorCode = m_sessionsStore.loadActiveSession(msg.payload().userName(), clientId);
                    if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                        //7标识未存在session
                        failedNoSession(channel);
                        return false;
                    }
                    session = m_sessionsStore.getSession(clientId);
                }

               /* if (session.getDeleted() != 0) {
                    LOG.error("user {} session {} is deleted. login failure", msg.payload().userName(), clientId);
                    failedNoSession(channel);
                    return false;
                }*/

                if (session != null && session.getUsername().equals(msg.payload().userName())) {
//                    pwd = AES.AESDecrypt(pwd, session.getSecret(), true);
                } else {
                    LOG.error("客户端密码解密失败 {}", clientId);
                    failedNoSession(channel);
                    return false;
                }

                if (pwd == null) {
                    LOG.error("客户端密码解密失败 {}", clientId);
                    failedCredentials(channel);
                    return false;
                }
                session.setMqttVersion(mqttVersion);
            } else {
                LOG.error("客户端未提供任何密码，并且禁用了MQTT匿名模式 CId={}", clientId);
                failedCredentials(channel);
                return false;
            }
            IMap<String, String> map = Server.getServer().getHazelcastInstance().getMap(clientId);
            String token = map.get(clientId);
            if (StringUtil.isNullOrEmpty(token)) {
                Map<String, String> userTokenMap = m_messagesStore.findUserTokenByUserId(msg.payload().userName());
                if (Objects.nonNull(userTokenMap)
                        && !userTokenMap.isEmpty()
                        && userTokenMap.containsKey(clientId)) {
                    token = userTokenMap.get(clientId);
                    map.set(clientId, token);
                }
            }
            //TODO 测试 忽略验证
            if (!new String(pwd).equals(token)) {
                LOG.error("Authenticator has rejected the MQTT credentials CId={}, username={}, password={}",
                        clientId, msg.payload().userName(), new String(pwd));
                failedCredentials(channel);
                return false;
            }
            NettyUtils.userName(channel, msg.payload().userName());
            return true;
        } else {
            LOG.error("客户端未提供任何凭据，并且MQTT匿名模式已禁用。 CId={}", clientId);
            failedCredentials(channel);
            return false;
        }
    }

    private boolean sendAck(ConnectionDescriptor descriptor, MqttConnectMessage msg, final String clientId) {
        LOG.info("Sending connect ACK. CId={}", clientId);
        final boolean success = descriptor.assignState(DISCONNECTED, SENDACK);
        if (!success) {
            return false;
        }

        MqttConnAckMessage okResp;
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientId);
        boolean isSessionAlreadyStored = clientSession != null;

        String user = msg.payload().userName();
        long messageHead = m_messagesStore.getMessageHead(user);
        long friendHead = m_messagesStore.getFriendHead(user);
        long friendRqHead = m_messagesStore.getFriendRqHead(user);
        long settingHead = m_messagesStore.getSettingHead(user);
        WFCMessage.ConnectAckPayload payload = WFCMessage.ConnectAckPayload.newBuilder()
                .setMsgHead(messageHead)
                .setFriendHead(friendHead)
                .setFriendRqHead(friendRqHead)
                .setSettingHead(settingHead)
                .setServerTime(System.currentTimeMillis())
                .build();


        if (!msg.variableHeader().isCleanSession() && isSessionAlreadyStored) {
            okResp = connAckWithSessionPresent(CONNECTION_ACCEPTED, payload.toByteArray());
        } else {
            okResp = connAck(CONNECTION_ACCEPTED, payload.toByteArray());
        }

        descriptor.writeAndFlush(okResp);
        LOG.info("连接确认已发送。 CId={}", clientId);
        return true;
    }

    private void initializeKeepAliveTimeout(Channel channel, MqttConnectMessage msg, final String clientId) {
        int keepAlive = msg.variableHeader().keepAliveTimeSeconds();
        LOG.info("配置连接。 CId={}", clientId);
        NettyUtils.keepAlive(channel, keepAlive);
        // session.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(msg.variableHeader().isCleanSession());
        // session.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(msg.variableHeader().isCleanSession());
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());
        // used to track the client in the subscription and publishing phases.
        // session.attr(NettyUtils.ATTR_KEY_CLIENTID).set(msg.getClientID());
        NettyUtils.clientID(channel, clientId);
        int idleTime = Math.round(keepAlive * 1.5f);
        setIdleTime(channel.pipeline(), idleTime);

        LOG.debug("连接已配置 CId={}, keepAlive={}, cleanSession={}, idleTime={}",
                clientId, keepAlive, msg.variableHeader().isCleanSession(), idleTime);
    }

    private ClientSession createOrLoadClientSession(String username, ConnectionDescriptor descriptor, MqttConnectMessage msg,
                                                    String clientId) {
        final boolean success = descriptor.assignState(SENDACK, SESSION_CREATED);
        if (!success) {
            return null;
        }

        m_sessionsStore.loadUserSession(username, clientId);
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientId);
        boolean isSessionAlreadyStored = clientSession != null;
        if (isSessionAlreadyStored) {
            clientSession = m_sessionsStore.updateExistSession(username, clientId, null, msg.variableHeader().isCleanSession());
        } else {
            return null;
        }

        return clientSession;
    }

    private void failedBlocked(Channel session) {
        session.writeAndFlush(connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED));
        LOG.info("客户端{}  无法连接，使用被阻止。", session);
    }

    private void failedCredentials(Channel session) {
        session.writeAndFlush(connAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
        LOG.info("客户端 {} 无法使用错误的用户名或密码连接。", session);
    }

    private void failedNoSession(Channel session) {
        session.writeAndFlush(connAck(CONNECTION_REFUSED_SESSION_NOT_EXIST));
        LOG.info("客户端 {} 无法使用错误的用户名或密码连接。", session);
    }

    private void setIdleTime(ChannelPipeline pipeline, int idleTime) {
        if (pipeline.names().contains("idleStateHandler")) {
            pipeline.remove("idleStateHandler");
        }
        pipeline.addFirst("idleStateHandler", new IdleStateHandler(idleTime, 0, 0));
    }

    public void processPubAck(Channel channel, MqttPubAckMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.variableHeader().messageId();
        String username = NettyUtils.userName(channel);
        LOG.trace("正在检索 messageID <{}>", messageID);

        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.inFlightAcknowledged(messageID);

        String topic = inflightMsg.getTopic();
        InterceptAcknowledgedMessage wrapped = new InterceptAcknowledgedMessage(inflightMsg, topic, username, messageID);
        m_interceptor.notifyMessageAcknowledged(wrapped);
    }

    public static IMessagesStore.StoredMessage asStoredMessage(MqttPublishMessage msg) {
        // TODO ugly, too much array copy
        ByteBuf payload = msg.payload();
        byte[] payloadContent = readBytesAndRewind(payload);

        IMessagesStore.StoredMessage stored = new IMessagesStore.StoredMessage(payloadContent,
                msg.fixedHeader().qosLevel(), msg.variableHeader().topicName());
        stored.setRetained(msg.fixedHeader().isRetain());
        return stored;
    }

    //TODO 1.处理Client to Broker
    public void processPublish(Channel channel, MqttPublishMessage msg) {
        final MqttQoS qos = msg.fixedHeader().qosLevel();
        final String clientId = NettyUtils.clientID(channel);

        LOG.info("正在处理发布消息。 CId={}, topic={}, messageId={}, qos={}", clientId,
                msg.variableHeader().topicName(), msg.variableHeader().packetId(), qos);
        switch (qos) {
            case AT_MOST_ONCE:
                //not support
                break;
            case AT_LEAST_ONCE:
                this.qos1PublishHandler.receivedPublishQos1(channel, msg);
                break;
            case EXACTLY_ONCE:
                //not use
                break;
            default:
                LOG.error("Unknown QoS-Type:{}", qos);
                break;
        }
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored
     * message and publish to all interested subscribers.
     *
     * @param channel the channel of the incoming message.
     * @param msg     the decoded pubrel message.
     */
    public void processPubRel(Channel channel, MqttMessage msg) {
        //not use
    }

    public void processPubRec(Channel channel, MqttMessage msg) {
        //not use
    }

    public void processPubComp(Channel channel, MqttMessage msg) {
        //not use
    }

    public void processDisconnect(Channel channel, boolean isDup, boolean isRetain) throws InterruptedException {
        final String clientID = NettyUtils.clientID(channel);
        LOG.info("处理断开连接消息. CId={}, clearSession={}", clientID, isDup);
        channel.flush();

        if (clientID == null) {
            LOG.error("断开连接时发生错误! clientId 不存在!!! {} , {}", clientID, isDup);
            channel.close();
            return;
        }

        if (!isDup && !isRetain) {
            processConnectionLost(clientID, channel, isDup);
            return;
        }


        final ConnectionDescriptor existingDescriptor = this.connectionDescriptors.getConnection(clientID);
        if (existingDescriptor == null) {
            // 另一个客户端使用相同id删除描述符，我们必须退出
            channel.close();
            return;
        }

        if (existingDescriptor.doesNotUseChannel(channel)) {
            // 另一个客户端保存了它的描述符，退出
            LOG.warn("另一个客户端正在使用连接描述符. clientID={}", clientID);
            existingDescriptor.abort();
            return;
        }


        if (!dropStoredMessages(existingDescriptor, clientID)) {
            LOG.warn("无法删除存储的消息。关闭连接 clientID={}", clientID);
            existingDescriptor.abort();
            return;
        }

        //调用集群通知连接断开
        if (!notifyInterceptorDisconnected(existingDescriptor, clientID)) {
            LOG.warn("无法删除遗嘱消息。正在关闭连接。 CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!existingDescriptor.close()) {
            LOG.info("连接已关闭。 CId={}", clientID);
            return;
        }

        this.connectionDescriptors.removeConnection(existingDescriptor);

        LOG.info("DISCONNECT消息已处理。 CId={}", clientID);

        String username = NettyUtils.userName(channel);
        MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
        if (session != null) {
            m_messagesStore.updateUserOnlineSetting(session, false);
        }
        if (session != null) {
            forwardOnlineStatusEvent(username, clientID, session.getPlatform(), UserOnlineStatus.LOGOUT);
        }

        channel.closeFuture();

        //disconnect the session

        m_sessionsStore.sessionForClient(clientID).disconnect(isDup, isRetain);
    }


    private boolean dropStoredMessages(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ESTABLISHED, MESSAGES_DROPPED);
        if (!success) {
            return false;
        }

        LOG.debug("删除会话消息。 CId={}", descriptor.clientID);
        this.m_sessionsStore.dropQueue(clientID);
        LOG.debug("该会话的消息已被删除。 CId={}", descriptor.clientID);

        return true;
    }

    private boolean notifyInterceptorDisconnected(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(MESSAGES_DROPPED, INTERCEPTORS_NOTIFIED);
        if (!success) {
            return false;
        }

        LOG.info("将删除消息。 ClientId={}", descriptor.clientID);
        // cleanup the will store
        String username = descriptor.getUsername();
        m_interceptor.notifyClientDisconnected(clientID, username);
        return true;
    }

    public void processConnectionLost(String clientID, Channel channel, boolean clearSession) {
        LOG.info("正在处理连接丢失事件。 CId={}", clientID);

        String username = NettyUtils.userName(channel);
        MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
        if (session != null) {
            session.refreshLastActiveTime();
            forwardOnlineStatusEvent(username, clientID, session.getPlatform(), clearSession ? UserOnlineStatus.LOGOUT : UserOnlineStatus.OFFLINE);
            m_messagesStore.updateUserOnlineSetting(session, false);
        }

        ConnectionDescriptor oldConnDescr = new ConnectionDescriptor(clientID, channel);
        if (connectionDescriptors.removeConnection(oldConnDescr)) {
            m_interceptor.notifyClientConnectionLost(clientID, username);
        }

    }

    /**
     * Remove the clientID from topic subscription, if not previously subscribed, doesn't reply any
     * error.
     *
     * @param channel the channel of the incoming message.
     * @param msg     the decoded unsubscribe message.
     */
    public void processUnsubscribe(Channel channel, MqttUnsubscribeMessage msg) {
        //Not use
    }

    public void processSubscribe(Channel channel, MqttSubscribeMessage msg) {
        //not use
    }


    /**
     * Create the SUBACK response from a list of topicFilters
     */
    private MqttSubAckMessage doAckMessageFromValidateFilters(List<MqttTopicSubscription> topicFilters, int messageId) {
        List<Integer> grantedQoSLevels = new ArrayList<>();
        for (MqttTopicSubscription req : topicFilters) {
            grantedQoSLevels.add(req.qualityOfService().value());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        return new MqttSubAckMessage(fixedHeader, from(messageId), payload);
    }

    public void notifyChannelWritable(Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        boolean emptyQueue = false;
        while (channel.isWritable() && !emptyQueue) {
            StoredMessage msg = clientSession.queue().poll();
            if (msg == null) {
                emptyQueue = true;
            } else {
                // 从存储的发布队列中重新创建发布
                MqttPublishMessage pubMsg = createPublishForQos(msg.getTopic(), msg.getQos(), msg.getPayload(),
                        msg.isRetained(), 0);
                channel.write(pubMsg);
            }
        }
        channel.flush();
    }

    public void addInterceptHandler(InterceptHandler interceptHandler) {
        this.m_interceptor.addInterceptHandler(interceptHandler);
    }

    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        this.m_interceptor.removeInterceptHandler(interceptHandler);
    }

    public IMessagesStore getMessagesStore() {
        return m_messagesStore;
    }

    public ISessionsStore getSessionsStore() {
        return m_sessionsStore;
    }

    public MessagesPublisher getMessagesPublisher() {
        return messagesPublisher;
    }

    public void onRpcMsg(String fromUser, String clientId, byte[] message, int messageId, String from, String request, boolean isAdmin) {
        if (request.equals(RPCCenter.KICKOFF_USER_REQUEST)) {
            String userId = new String(message);
            mServer.getImBusinessScheduler().execute(() -> handleTargetRemovedFromCurrentNode(new TargetEntry(TargetEntry.Type.TARGET_TYPE_USER, userId)));
            return;
        }
        qos1PublishHandler.onRpcMsg(fromUser, clientId, message, messageId, from, request, isAdmin);
    }

    public void shutdown() {
        messagesPublisher.stopChatroomScheduler();
    }
}

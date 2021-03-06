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

package io.moquette.server;

import cn.wildfirechat.common.ErrorCode;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.moquette.BrokerConstants;
import io.moquette.connections.ClusterConnectionsManager;
import io.moquette.connections.IConnectionsManager;
import io.moquette.connections.MqttConnectionMetrics;
import io.moquette.connections.MqttSession;
import io.moquette.server.config.IConfig;
import io.moquette.server.netty.metrics.BytesMetrics;
import io.moquette.server.netty.metrics.MessageMetrics;
import io.moquette.spi.ClientSession;
import io.moquette.spi.ISessionsStore;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.moquette.liuyan.contants.LaoLiuConstants.HazelcastConstants.CLUSTER_CONNECTION_MAP;
import static io.moquette.liuyan.contants.LaoLiuConstants.HazelcastConstants.NON_EXIST_NODE;

public class ConnectionDescriptorStore implements IConnectionsManager, ClusterConnectionsManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionDescriptorStore.class);

    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
    private final ISessionsStore sessionsStore;

    //TODO: 2020???12???15???20???03?????????
    private final HazelcastInstance hazelcastInstance;

    public ConnectionDescriptorStore(ISessionsStore sessionsStore) {
        this.connectionDescriptors = new ConcurrentHashMap<>();
        this.sessionsStore = sessionsStore;
        this.hazelcastInstance = Server.getServer().getHazelcastInstance();
    }

    public boolean sendMessage(MqttMessage message, Integer messageID, String clientID, ErrorCode errorCode) {
        final MqttMessageType messageType = message.fixedHeader().messageType();
        try {
            if (messageID != null) {
                LOG.info("Sending {} message clientID=<{}>, messageId={}, errorCode={}", messageType, clientID, messageID, errorCode);
            } else {
                LOG.debug("Sending {} message clientID=<{}>", messageType, clientID);
            }

            ConnectionDescriptor descriptor = connectionDescriptors.get(clientID);
            if (descriptor == null) {
                if (messageID != null) {
                    LOG.error("??????????????????????????? {} ?????????????????? clientID=<{}>, messageId={}", messageType, clientID, messageID);
                } else {
                    LOG.error("??????????????????????????? {} ????????????????????? clientID=<{}>", messageType, clientID);
                }
                /*
                 * ????????????????????????????????????????????????????????????????????????????????????broker??????:???????????????????????????
                 */
                return false;
            }
            //TODO  ????????????
            descriptor.writeAndFlush(message);
            return true;
        } catch (Throwable e) {
            String errorMsg = "???????????? " + messageType + " ???????????????. clientID=<" + clientID + ">";
            if (messageID != null) {
                errorMsg += ", messageId=" + messageID;
            }
            LOG.error(errorMsg, e);
            return false;
        }
    }

    public ConnectionDescriptor addConnection(ConnectionDescriptor descriptor) {
        //TODO: 2020???12???16???10???31??????????????????????????????????????????
        IConfig iConfig = Server.defaultConfig();
        String nodeIdStr = iConfig.getProperty(BrokerConstants.NODE_ID);
        IMap<String, String> clusterMap = hazelcastInstance.getMap(CLUSTER_CONNECTION_MAP);
        clusterMap.put(descriptor.clientID, nodeIdStr);

        return connectionDescriptors.putIfAbsent(descriptor.clientID, descriptor);
    }

    /**
     * TODO:
     * 1. ??????????????????????????????clientId(???????????????)
     * 2. ??????????????????,????????????????????????clientId,??????????????????
     * 3. ????????????????????????,??????????????????clientId
     *
     * @param descriptor
     * @return
     */
    public boolean removeConnection(ConnectionDescriptor descriptor) {
        //TODO: 2020???12???16???10???40????????????????????????????????????????????????
        IMap<String, String> clusterMap = hazelcastInstance.getMap(CLUSTER_CONNECTION_MAP);
        clusterMap.delete(descriptor.clientID);

        return connectionDescriptors.remove(descriptor.clientID, descriptor);
    }

    /**
     * TODO:
     * 1. ??????clientId?????????????????????
     *
     * @param clientID
     * @return
     */
    public ConnectionDescriptor getConnection(String clientID) {
        return connectionDescriptors.get(clientID);
    }

    @Override
    public boolean isConnected(String clientID) {
        return connectionDescriptors.containsKey(clientID);
    }

    @Override
    public int getActiveConnectionsNo() {
        return connectionDescriptors.size();
    }

    @Override
    public Collection<String> getConnectedClientIds() {
        return connectionDescriptors.keySet();
    }

    /**
     * TODO: 2020???12???16???11???22?????????  ??????????????????????????????????????????
     *
     * @param clientId ?????????????????????Id
     * @return True Or False
     */
    @Override
    public boolean isConnectedCluster(String clientId) {
        return hazelcastInstance.getMap(CLUSTER_CONNECTION_MAP).containsKey(clientId);
    }

    /**
     * ???????????????????????????????????????
     *
     * @return ?????????
     */
    @Override
    public int getClusterActiveConnectionsNo() {
        return hazelcastInstance.getMap(CLUSTER_CONNECTION_MAP).size();
    }

    /**
     * ??????clientId????????????????????????id
     *
     * @param clientId ?????????id
     * @return ??????????????????NodeId
     */
    @Override
    public int getClusterNodeByClientId(String clientId) {
        IMap<String, String> clusterMap = hazelcastInstance.getMap(CLUSTER_CONNECTION_MAP);
        String nodeIdStr = clusterMap.get(clientId);
        if (StringUtil.isNullOrEmpty(nodeIdStr)) {
            return NON_EXIST_NODE;
        }
        return Integer.parseInt(nodeIdStr);
    }

    @Override
    public boolean closeConnection(String clientID, boolean closeImmediately) {
        ConnectionDescriptor descriptor = connectionDescriptors.get(clientID);
        if (descriptor == null) {
            LOG.error("???????????????????????????????????????MQTT????????? clientID=<{}>, closeImmediately={}", clientID, closeImmediately);
            return false;
        }
        if (closeImmediately) {
            descriptor.abort();
            return true;
        } else {
            return descriptor.close();
        }
    }

    @Override
    public MqttSession getSessionStatus(String clientID) {
        LOG.info("getSessionStatus ????????????????????? clientID=<{}>", clientID);
        ClientSession session = sessionsStore.sessionForClient(clientID);
        if (session == null) {
            LOG.error("MQTT?????????ID???????????????????????? clientID=<{}>", clientID);
            return null;
        }
        return buildMqttSession(session);
    }

    @Override
    public Collection<MqttSession> getSessions() {
        LOG.info("???????????????????????????");
        Collection<MqttSession> result = new ArrayList<>();
        for (ClientSession session : sessionsStore.getAllSessions()) {
            result.add(buildMqttSession(session));
        }
        return result;
    }

    private MqttSession buildMqttSession(ClientSession session) {
        MqttSession result = new MqttSession();

        result.setCleanSession(true);
        ConnectionDescriptor descriptor = this.getConnection(session.clientID);
        if (descriptor != null) {
            result.setConnectionEstablished(true);
            BytesMetrics bytesMetrics = descriptor.getBytesMetrics();
            MessageMetrics messageMetrics = descriptor.getMessageMetrics();
            result.setConnectionMetrics(new MqttConnectionMetrics(bytesMetrics.readBytes(), bytesMetrics.wroteBytes(),
                    messageMetrics.messagesRead(), messageMetrics.messagesWrote()));
        } else {
            result.setConnectionEstablished(false);
        }
        result.setPendingPublishMessagesNo(session.getPendingPublishMessagesNo());
        result.setSecondPhaseAckPendingMessages(session.getSecondPhaseAckPendingMessages());
        result.setInflightMessages(session.getInflightMessagesNo());
        return result;
    }

}

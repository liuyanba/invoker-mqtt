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

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.Interceptor;
import io.moquette.interception.messages.*;
import io.moquette.server.config.IConfig;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.moquette.logging.LoggingUtils.getInterceptorIds;

/**
 * An interceptor that execute the interception tasks asynchronously.
 */
final class BrokerInterceptor implements Interceptor {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerInterceptor.class);
    private final Map<Class<?>, List<InterceptHandler>> handlers;
    private final ExecutorService executor;

    private BrokerInterceptor(int poolSize, List<InterceptHandler> handlers) {
        LOG.info("初始化代理拦截器。 InterceptorIds={}", getInterceptorIds(handlers));
        //TODO:从这个map可以看出来，moquette允许你在，各个关键阶段注册一系列的处理器，供它回调。value是拦截器的list
        this.handlers = new HashMap<>();
        for (Class<?> messageType : InterceptHandler.ALL_MESSAGE_TYPES) {
            this.handlers.put(messageType, new CopyOnWriteArrayList<>());
        }
        for (InterceptHandler handler : handlers) {
            this.addInterceptHandler(handler);
        }
        executor = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Configures a broker interceptor, with a thread pool of one thread.
     *
     * @param handlers
     */
    BrokerInterceptor(List<InterceptHandler> handlers) {
        this(1, handlers);
    }

    /**
     * Configures a broker interceptor using the pool size specified in the IConfig argument.
     */
    BrokerInterceptor(IConfig props, List<InterceptHandler> handlers) {
        this(Integer.parseInt(props.getProperty(BrokerConstants.BROKER_INTERCEPTOR_THREAD_POOL_SIZE, "1")), handlers);
    }

    /**
     * Shutdown graciously the executor service
     */
    void stop() {
        LOG.info("正在关闭拦截器线程池...");
        executor.shutdown();
        try {
            LOG.info("等待线程池任务终止...");
            executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!executor.isTerminated()) {
            LOG.warn("强制关闭拦截器线程池...");
            executor.shutdownNow();
        }
    }

    @Override
    public void notifyClientConnected(final MqttConnectMessage msg) {
        for (final InterceptHandler handler : this.handlers.get(InterceptConnectMessage.class)) {
            LOG.debug("发送MQTT CONNECT消息到拦截器。 CId={}, interceptorId={}",
                    msg.payload().clientIdentifier(), handler.getID());
            executor.execute(() -> handler.onConnect(new InterceptConnectMessage(msg)));
        }
    }

    @Override
    public void notifyClientDisconnected(final String clientID, final String username) {
        for (final InterceptHandler handler : this.handlers.get(InterceptDisconnectMessage.class)) {
            LOG.debug("通知MQTT客户端断开与拦截器的连接。 CId={}, username={}, interceptorId={}",
                    clientID, username, handler.getID());
            executor.execute(() -> handler.onDisconnect(new InterceptDisconnectMessage(clientID, username)));
        }
    }

    @Override
    public void notifyClientConnectionLost(final String clientID, final String username) {
        for (final InterceptHandler handler : this.handlers.get(InterceptConnectionLostMessage.class)) {
            LOG.debug("通知MQTT客户端意外断开与拦截器的连接 CId={}, username={}, " +
                    "interceptorId={}", clientID, username, handler.getID());
            executor.execute(() -> handler.onConnectionLost(new InterceptConnectionLostMessage(clientID, username)));
        }
    }

    @Override
    public void notifyMessageAcknowledged(final InterceptAcknowledgedMessage msg) {
        for (final InterceptHandler handler : this.handlers.get(InterceptAcknowledgedMessage.class)) {
            LOG.debug("通知MQTT ACK消息给拦截器。 CId={}, messageId={}, topic={}, interceptorId={}",
                    msg.getMsg().getClientID(), msg.getPacketID(), msg.getTopic(), handler.getID());
            executor.execute(() -> handler.onMessageAcknowledged(msg));
        }
    }

    @Override
    public void addInterceptHandler(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = getInterceptedMessageTypes(interceptHandler);
        LOG.info("新增MQTT集群消息拦截器!!!!  InterceptorId={}, handledMessageTypes={}",
                interceptHandler.getID(), interceptedMessageTypes);
        for (Class<?> interceptMessageType : interceptedMessageTypes) {
            this.handlers.get(interceptMessageType).add(interceptHandler);
        }
    }

    @Override
    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = getInterceptedMessageTypes(interceptHandler);
        LOG.info("除去MQTT消息拦截器。 InterceptorId={}, handledMessageTypes={}",
                interceptHandler.getID(), interceptedMessageTypes);
        for (Class<?> interceptMessageType : interceptedMessageTypes) {
            this.handlers.get(interceptMessageType).remove(interceptHandler);
        }
    }

    private static Class<?>[] getInterceptedMessageTypes(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = interceptHandler.getInterceptedMessageTypes();
        if (interceptedMessageTypes == null) {
            return InterceptHandler.ALL_MESSAGE_TYPES;
        }
        return interceptedMessageTypes;
    }

    public void notifyTopicPublished(final MqttPublishMessage msg, final String clientID, final String username) {
        int messageId = msg.variableHeader().packetId();
        String topic = msg.variableHeader().topicName();
        for (final InterceptHandler handler : this.handlers.get(InterceptPublishMessage.class)) {
            LOG.info("开始推送至MQTT消息到拦截器! CId={}, messageId={}, topic={}, interceptorId={}",
                    clientID, messageId, topic, handler.getID());
            msg.content().retain();
            executor.execute(() -> handler.onPublish(new InterceptPublishMessage(msg, clientID, username)));
        }
    }
}

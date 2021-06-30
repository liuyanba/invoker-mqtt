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

package io.moquette.interception;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.spi.impl.Utils.readBytesAndRewind;

public class HazelcastInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastInterceptHandler.class);
    private final HazelcastInstance hz;

    public HazelcastInterceptHandler(Server server) {
        this.hz = server.getHazelcastInstance();
    }

    @Override
    public String getID() {
        return HazelcastInterceptHandler.class.getName() + "@" + hz.getName();
    }

    /**
     * TODO:
     * 它是注册了七个事件，brokerintercept也确实都回调了它，
     * 但是它只实现了这一个方法，其他的方法都是通过抽象方法继承来的空方法，也就是什么都没做。
     * 这样当然扩展性稍微好一点，主动权在处理器上面，但其实没有太大的必要。
     * 因为每个处理器想监听哪些事件，其实是知道的，监听几个注册几个就完了。
     * @param msg
     */
    @Override
    public void onPublish(InterceptPublishMessage msg) {
        // TODO ugly, too much array copy
        ByteBuf payload = msg.getPayload();
        byte[] payloadContent = readBytesAndRewind(payload);

        LOG.info("开始推送消息至其他节点: {} publish on {} message: {}", msg.getClientID(), msg.getTopicName(), new String(payloadContent));
        ITopic<HazelcastMsg> topic = hz.getTopic("moquette");
        HazelcastMsg hazelcastMsg = new HazelcastMsg(msg);
        topic.publish(hazelcastMsg);
        LOG.info("已推送消息至集群通讯topic:{}    消息体:{}","moquette", new Gson().toJson(hazelcastMsg));
    }

}

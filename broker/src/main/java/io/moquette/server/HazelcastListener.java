/*
 * Copyright (c) 2012-2018 The original author or authors
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
import com.alibaba.fastjson.JSON;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.moquette.imhandler.IMHandler;
import io.moquette.interception.HazelcastMsg;
import io.moquette.spi.impl.MessagesPublisher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.IMTopic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class HazelcastListener implements MessageListener<HazelcastMsg> {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastListener.class);

    private final Server server;
    private static final CopyOnWriteArrayList<String> notifyTopicList = new CopyOnWriteArrayList<>();

    public HazelcastListener(Server server) {
        this.server = server;
        notifyTopicList.add(IMTopic.NotifyFriendTopic);
        notifyTopicList.add(IMTopic.NotifyMessageTopic);
        notifyTopicList.add(IMTopic.NotifyRecallMessageTopic);
        notifyTopicList.add(IMTopic.NotifyUserSettingTopic);
        notifyTopicList.add(IMTopic.NotifyFriendRequestTopic);
    }

    //TODO:集群间的消息同步，其实就是模仿的client向broker发送publish消息
    @Override
    public void onMessage(Message<HazelcastMsg> msg) {
        ByteBuf payload = null;
        try {
            //TODO: 除自己外处理
            if (!msg.getPublishingMember().equals(server.getHazelcastInstance().getCluster().getLocalMember())) {
                HazelcastMsg hzMsg = msg.getMessageObject();
                // TODO pass forward this information in somehow publishMessage.setLocal(false);
                String topic = hzMsg.getTopic();
                payload = Unpooled.wrappedBuffer(hzMsg.getPayload());
                LOG.info("接受到集群消息 :  ClientId={} ,topic={},msg={},payload={}", hzMsg.getClientId(), topic,
                        JSON.toJSON(msg), new String(hzMsg.getPayload()));

                ByteBuf ackPayload = Unpooled.buffer(1);
                ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
                ackPayload.ensureWritable(1).writeByte(errorCode.getCode());

                //写楚ackPayload
//                server.getProcessor().qos1PublishHandler.imHandler(
//                        hzMsg.getClientId(),
//                        hzMsg.getUsername(),
//                        topic,
//                        hzMsg.getPayload(),
//                        (errorCode1, ackPayload1) -> server.getProcessor().qos1PublishHandler.sendPubAck(hzMsg.getClientId(),hzMsg.getMessageId(), ackPayload, errorCode1),
//                        false);

                HashMap<String, IMHandler> imHandlerHashMap = server.getProcessor().qos1PublishHandler.getImHandlers();
                IMHandler handler = imHandlerHashMap.get(topic);
                //如果存在topic 并且topic内容以n结尾 那么说明是通知
                if (Objects.nonNull(handler)) {
                    //TODO: 我们自己的处理可以重写该receiveAction方法
                    errorCode = handler.receiveAction(ackPayload, hzMsg.getClientId(), hzMsg.getUsername(), handler.getDataObject(hzMsg.getPayload()));
                    LOG.info("已消费集群消息 publish result code={},msg={}", errorCode.getCode(), errorCode.getMsg());
                }
                MessagesPublisher messagesPublisher = server.getProcessor().getMessagesPublisher();
                boolean isNotifyTopic = !Objects.isNull(topic) && !StringUtil.isNullOrEmpty(topic) && notifyTopicList.contains(topic);
                //特殊通知  带文字的
                if (isNotifyTopic && IMTopic.NotifyMessageTopic.equals(topic)) {
                    messagesPublisher.publish2ReceiversNew(hzMsg.getUsername(),0);
                    return;
                }

                //正常通知  long类型的
                if (isNotifyTopic) {
                    //通知
                    messagesPublisher.publishNotificationLocalClientId(topic, hzMsg.getClientId(), payloadGetLongHead(hzMsg.getPayload()));
                }
                // TODO: 2020/8/13  ackPayload是否需要写回channel 待定
            }
        } catch (Exception ex) {
            LOG.error("错误轮询-msg队列", ex);
        } finally {
            ReferenceCountUtil.release(payload);
        }
    }

    private static long payloadGetLongHead(byte[] array) {
        ByteArrayInputStream bai = new ByteArrayInputStream(array);
        DataInputStream dis = new DataInputStream(bai);
        try {
            return dis.readLong();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

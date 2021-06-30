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

package io.moquette.server.netty;

import com.xiaoleilu.hutool.util.DateUtil;
import io.moquette.spi.impl.ProtocolProcessor;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

@Sharable
public class NettyMQTTHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NettyMQTTHandler.class);
    private final ProtocolProcessor m_processor;

    public NettyMQTTHandler(ProtocolProcessor processor) {
        m_processor = processor;
    }


    /**
     * <table>
     * <thead>
     * <tr>
     * <th><strong>名字</strong></th>
     * <th><strong>值</strong></th>
     * <th><strong>报文流动方向</strong></th>
     * <th><strong>描述</strong></th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>Reserved</td>
     * <td>0</td>
     * <td>禁止</td>
     * <td>保留</td>
     * </tr>
     * <tr>
     * <td>CONNECT</td>
     * <td>1</td>
     * <td>客户端到服务端</td>
     * <td>客户端请求连接服务端</td>
     * </tr>
     * <tr>
     * <td>CONNACK</td>
     * <td>2</td>
     * <td>服务端到客户端</td>
     * <td>连接报文确认</td>
     * </tr>
     * <tr>
     * <td>PUBLISH</td>
     * <td>3</td>
     * <td>两个方向都允许</td>
     * <td>发布消息</td>
     * </tr>
     * <tr>
     * <td>PUBACK</td>
     * <td>4</td>
     * <td>两个方向都允许</td>
     * <td>QoS 1消息发布收到确认</td>
     * </tr>
     * <tr>
     * <td>PUBREC</td>
     * <td>5</td>
     * <td>两个方向都允许</td>
     * <td>发布收到（保证交付第一步）</td>
     * </tr>
     * <tr>
     * <td>PUBREL</td>
     * <td>6</td>
     * <td>两个方向都允许</td>
     * <td>发布释放（保证交付第二步）</td>
     * </tr>
     * <tr>
     * <td>PUBCOMP</td>
     * <td>7</td>
     * <td>两个方向都允许</td>
     * <td>QoS 2消息发布完成（保证交互第三步）</td>
     * </tr>
     * <tr>
     * <td>SUBSCRIBE</td>
     * <td>8</td>
     * <td>客户端到服务端</td>
     * <td>客户端订阅请求</td>
     * </tr>
     * <tr>
     * <td>SUBACK</td>
     * <td>9</td>
     * <td>服务端到客户端</td>
     * <td>订阅请求报文确认</td>
     * </tr>
     * <tr>
     * <td>UNSUBSCRIBE</td>
     * <td>10</td>
     * <td>客户端到服务端</td>
     * <td>客户端取消订阅请求</td>
     * </tr>
     * <tr>
     * <td>UNSUBACK</td>
     * <td>11</td>
     * <td>服务端到客户端</td>
     * <td>取消订阅报文确认</td>
     * </tr>
     * <tr>
     * <td>PINGREQ</td>
     * <td>12</td>
     * <td>客户端到服务端</td>
     * <td>心跳请求</td>
     * </tr>
     * <tr>
     * <td>PINGRESP</td>
     * <td>13</td>
     * <td>服务端到客户端</td>
     * <td>心跳响应</td>
     * </tr>
     * <tr>
     * <td>DISCONNECT</td>
     * <td>14</td>
     * <td>客户端到服务端</td>
     * <td>客户端断开连接</td>
     * </tr>
     * <tr>
     * <td>Reserved</td>
     * <td>15</td>
     * <td>禁止</td>
     * <td>保留</td>
     * </tr>
     * </tbody>
     * </table>
     */
    //TODO MQTT协议处理 netty与其他应用的接入点
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        try {
            if (!(message instanceof MqttMessage)) {
                LOG.error("未知的MQTT消息类型 {}, {}", message.getClass().getName(), message);
                return;
            }
            MqttMessage msg = (MqttMessage) message;
            MqttMessageType messageType = msg.fixedHeader().messageType();
            LOG.info("Processing MQTT message, type={} ClinetId={} Date={}", messageType, NettyUtils.clientID(ctx.channel()), DateUtil.now());
            System.out.println("=================类型" + messageType + "ClinetId=" + NettyUtils.clientID(ctx.channel()) + "日期==" + DateUtil.now());
            switch (messageType) {
                case CONNECT:
                    m_processor.processConnect(ctx.channel(), (MqttConnectMessage) msg);
                    break;
                case SUBSCRIBE:
                    m_processor.processSubscribe(ctx.channel(), (MqttSubscribeMessage) msg);
                    break;
                case UNSUBSCRIBE:
                    m_processor.processUnsubscribe(ctx.channel(), (MqttUnsubscribeMessage) msg);
                    break;
                case PUBLISH:
                    m_processor.processPublish(ctx.channel(), (MqttPublishMessage) msg);
                    break;
                case PUBREC:
                    m_processor.processPubRec(ctx.channel(), msg);
                    break;
                case PUBCOMP:
                    m_processor.processPubComp(ctx.channel(), msg);
                    break;
                case PUBREL:
                    m_processor.processPubRel(ctx.channel(), msg);
                    break;
                case DISCONNECT:
                    m_processor.processDisconnect(ctx.channel(), msg.fixedHeader().isDup(), msg.fixedHeader().isRetain());
                    break;
                case PUBACK:
                    m_processor.processPubAck(ctx.channel(), (MqttPubAckMessage) msg);
                    break;
                case PINGREQ:
                    MqttFixedHeader pingHeader = new MqttFixedHeader(
                            MqttMessageType.PINGRESP,
                            false,
                            AT_MOST_ONCE,
                            false,
                            0);
                    MqttMessage pingResp = new MqttMessage(pingHeader);
                    ctx.writeAndFlush(pingResp);
                    break;
                default:
                    LOG.error("未知的 MessageType:{}", messageType);
                    break;
            }
        } catch (Throwable ex) {
            LOG.error("处理MQTT消息时捕获到异常, " + ex.getCause(), ex);
            ctx.fireExceptionCaught(ex);
            ctx.close();
        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientID = NettyUtils.clientID(ctx.channel());
        if (clientID != null && !clientID.isEmpty()) {
            LOG.info("通知连接丢失事件。 MqttClientId = {}.", clientID);
            m_processor.processConnectionLost(clientID, ctx.channel(), false);
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("处理MQTT消息时捕获到意外的异常。关闭Netty通道。 CId={}, " +
                "cause={}, errorMessage={}", NettyUtils.clientID(ctx.channel()), cause.getCause(), cause.getMessage());
        for (StackTraceElement ste : cause.getStackTrace()) {
            LOG.error(ste.toString());
        }
        ctx.close();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (ctx.channel().isWritable()) {
            m_processor.notifyChannelWritable(ctx.channel());
        }
        ctx.fireChannelWritabilityChanged();
    }

}

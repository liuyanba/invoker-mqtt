/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.ProtoUtil;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.netty.buffer.ByteBuf;
import com.liuyan.im.IMTopic;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(value = IMTopic.PullMessageTopic)
public class PullMessageHandler extends IMHandler<WFCMessage.PullMessageRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullMessageRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;

        if (request.getType() == ProtoConstants.PullType.Pull_ChatRoom && !m_messagesStore.checkUserClientInChatroom(fromUser, clientID, null)) {
            errorCode = ErrorCode.ERROR_CODE_NOT_IN_CHATROOM;
        } else {
            //TODO 拉取消息 通过用户和会话拉取？
            WFCMessage.PullMessageResult result = m_messagesStore.fetchMessage(fromUser, clientID, request.getId(), request.getType());
            // byte[] data = result.toByteArray();
            String resultJson = ProtoUtil.toJson(WFCMessage.PullMessageResult.class, result);
            System.out.println("拉取的指定消息：" + resultJson);
            LOG.info("MP  拉取的指定消息 LIU YAN Result:{}", resultJson);
            byte[] bytes = resultJson.getBytes();
            LOG.info("具有计数({})、有效载荷大小({})的用户{}拉消息", fromUser, result.getMessageCount(), bytes.length);
            ackPayload.ensureWritable(bytes.length).writeBytes(bytes);
        }
        return errorCode;
    }
}

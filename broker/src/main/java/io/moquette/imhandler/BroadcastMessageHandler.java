/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import com.liuyan.im.IMTopic;
import com.liuyan.im.MessageShardingUtil;

@Handler(value = IMTopic.BroadcastMessageTopic)
public class BroadcastMessageHandler extends IMHandler<WFCMessage.Message> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Message message, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        if (message != null) {
            if (!isAdmin) {  //only admin can broadcast message
                return ErrorCode.ERROR_CODE_NOT_RIGHT;
            }

            long timestamp = System.currentTimeMillis();
            long messageId = MessageShardingUtil.generateId();
            message = message.toBuilder().setFromUser(fromUser).setMessageId(messageId).setServerTimestamp(timestamp).setConversation(WFCMessage.Conversation.newBuilder().setTarget(fromUser).setLine(message.getConversation().getLine()).setType(ProtoConstants.ConversationType.ConversationType_Private).build()).build();

            long count = saveAndBroadcast(fromUser, clientID, message);
            ackPayload = ackPayload.capacity(20);
            ackPayload.writeLong(messageId);
            ackPayload.writeLong(count);
        } else {
            errorCode = ErrorCode.ERROR_CODE_INVALID_MESSAGE;
        }
        return errorCode;
    }

}

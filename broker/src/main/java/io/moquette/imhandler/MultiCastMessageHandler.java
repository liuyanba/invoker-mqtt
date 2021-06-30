/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.WFCMessage;
import io.netty.buffer.ByteBuf;
import com.liuyan.im.IMTopic;
import com.liuyan.im.MessageShardingUtil;

import static cn.wildfirechat.proto.ProtoConstants.ConversationType.ConversationType_Private;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(value = IMTopic.MultiCastMessageTopic)
public class MultiCastMessageHandler extends IMHandler<WFCMessage.MultiCastMessage> {


    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.MultiCastMessage multiCastMessage, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        if (!isAdmin) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        long timestamp = System.currentTimeMillis();
        long messageId = MessageShardingUtil.generateId();
        WFCMessage.Message message = WFCMessage.Message.newBuilder()
            .setContent(multiCastMessage.getContent())
            .setConversation(WFCMessage.Conversation.newBuilder().setTarget(fromUser).setType(ConversationType_Private).setLine(multiCastMessage.getLine()).build())
            .setFromUser(fromUser)
            .setMessageId(messageId)
            .setServerTimestamp(timestamp)
            .build();

        saveAndMulticast(fromUser, clientID, message, multiCastMessage.getToList());

        ackPayload = ackPayload.capacity(20);
        ackPayload.writeLong(messageId);
        ackPayload.writeLong(timestamp);

        return errorCode;
    }

}

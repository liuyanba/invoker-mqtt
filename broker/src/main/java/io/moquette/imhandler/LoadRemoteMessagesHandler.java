/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.ProtoUtil;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;
import io.moquette.spi.impl.Qos1PublishHandler;
//TODO 拉取历史消息
@Handler(value = IMTopic.LoadRemoteMessagesTopic)
public class LoadRemoteMessagesHandler extends IMHandler<WFCMessage.LoadRemoteMessages> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.LoadRemoteMessages request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        System.out.println("===============LRM request:" + request);
        long beforeUid = request.getBeforeUid();
        if (beforeUid == 0) {
            beforeUid = Long.MAX_VALUE;
        }

        if (request.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group) {
            if (!isAdmin && !m_messagesStore.isMemberInGroup(fromUser, request.getConversation().getTarget())) {
                return ErrorCode.ERROR_CODE_NOT_IN_GROUP;
            }
        }

        if (request.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel) {
            if (!m_messagesStore.checkUserInChannel(fromUser, request.getConversation().getTarget())) {
                return ErrorCode.ERROR_CODE_NOT_IN_CHANNEL;
            }
        }

        WFCMessage.PullMessageResult result = m_messagesStore.loadRemoteMessages(fromUser, request.getConversation(), beforeUid, request.getCount());
        String jsonResult = ProtoUtil.toJson(WFCMessage.PullMessageResult.class, result);
        System.out.println(jsonResult);
        LOG.info("LRM  LIU_YAN_Result 返回数据:{}" ,jsonResult);
        byte[] data = jsonResult.getBytes();
        LOG.info("User {} load message with count({}), payload size({})", fromUser, result.getMessageCount(), data.length);
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return errorCode;
    }
}

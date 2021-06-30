/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.pojos.GroupNotificationBinaryContent;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(value = IMTopic.AddGroupMemberTopic)
public class AddGroupMember extends GroupHandler<WFCMessage.AddGroupMemberRequest> {

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.AddGroupMemberRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.addGroupMembers(fromUser, isAdmin, request.getGroupId(), request.getAddedMemberList());
        if (errorCode == ERROR_CODE_SUCCESS) {
            sendNotification(fromUser, request);
        }

        return errorCode;
    }

    @Override
    public ErrorCode receiveAction(ByteBuf ackPayload, String clientID, String fromUser, WFCMessage.AddGroupMemberRequest request) {
        sendNotification(fromUser, request);
        return ERROR_CODE_SUCCESS;
    }

    private void sendNotification(String fromUser, WFCMessage.AddGroupMemberRequest request) {
        if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
            sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
        } else {
            WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, getMemberIdList(request.getAddedMemberList())).getAddGroupNotifyContent();
            sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
        }
    }
}

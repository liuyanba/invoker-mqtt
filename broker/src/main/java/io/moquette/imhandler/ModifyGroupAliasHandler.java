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
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(IMTopic.ModifyGroupAliasTopic)
public class ModifyGroupAliasHandler extends GroupHandler<WFCMessage.ModifyGroupMemberAlias> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyGroupMemberAlias request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.modifyGroupAlias(fromUser, request.getGroupId(), request.getAlias());
        if (errorCode == ERROR_CODE_SUCCESS && request.hasNotifyContent()) {
            if (request.hasNotifyContent()&& request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getAlias(), "").getModifyGroupMemberAliasNotifyContent();
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
            }
        }
        return errorCode;
    }
}

/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(IMTopic.SetFriendAliasTopic)
public class SetFriendAliasRequestHandler extends GroupHandler<WFCMessage.AddFriendRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.AddFriendRequest request, Qos1PublishHandler.IMCallback callback) {
        long[] head = new long[1];
        ErrorCode errorCode = m_messagesStore.setFriendAliasRequest(fromUser, request.getTargetUid(), request.getReason(), head);
        if (errorCode == ERROR_CODE_SUCCESS) {
            publisher.publishNotification(IMTopic.NotifyFriendTopic, fromUser, head[0]);
        }
        return errorCode;
    }
}

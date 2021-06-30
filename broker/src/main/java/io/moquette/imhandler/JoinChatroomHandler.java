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
import io.moquette.spi.impl.Qos1PublishHandler;

@Handler(IMTopic.JoinChatroomTopic)
public class JoinChatroomHandler extends IMHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        return m_messagesStore.handleJoinChatroom(fromUser, clientID, request.getId());
    }
}

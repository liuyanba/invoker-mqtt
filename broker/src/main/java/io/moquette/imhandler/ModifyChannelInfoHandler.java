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

import static com.liuyan.im.IMTopic.ModifyChannelInfoTopic;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(value = ModifyChannelInfoTopic)
public class ModifyChannelInfoHandler extends GroupHandler<WFCMessage.ModifyChannelInfo> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyChannelInfo request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode= m_messagesStore.modifyChannelInfo(fromUser, request.getChannelId(), request.getType(), request.getValue());
        return errorCode;
    }
}

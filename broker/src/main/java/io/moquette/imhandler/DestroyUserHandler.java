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
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(IMTopic.DestroyUserTopic)
public class DestroyUserHandler extends IMHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        if (isAdmin) {
            mServer.getImBusinessScheduler().execute(()-> {
                m_sessionsStore.clearUserSession(fromUser);
                m_messagesStore.clearUserMessages(fromUser);
                m_messagesStore.clearUserSettings(fromUser);
                m_messagesStore.clearUserFriend(fromUser);
                m_messagesStore.clearUserGroups(fromUser);
                m_messagesStore.clearUserChannels(fromUser);
                m_messagesStore.destoryUser(fromUser);
            });
            return ErrorCode.ERROR_CODE_SUCCESS;
        } else {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }
    }
}

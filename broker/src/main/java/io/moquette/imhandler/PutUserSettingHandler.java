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
@Handler(IMTopic.PutUserSettingTopic)
public class PutUserSettingHandler extends IMHandler<WFCMessage.ModifyUserSettingReq> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyUserSettingReq request, Qos1PublishHandler.IMCallback callback) {
        m_messagesStore.updateUserSettings(fromUser, request);
        LOG.info("UP LIU YAN Result:SUCCESS");
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode receiveAction(ByteBuf ackPayload, String clientID, String fromUser, WFCMessage.ModifyUserSettingReq request) {
        IMHandler.getPublisher().publishNotification(IMTopic.NotifyUserSettingTopic, fromUser, System.currentTimeMillis());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}

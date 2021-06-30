/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import static com.liuyan.im.IMTopic.PutUserSettingTopic;
import static com.liuyan.im.UserSettingScope.kUserSettingListenedChannels;

@Handler(value = IMTopic.ChannelListenTopic)
public class ChannelListenMember extends IMHandler<WFCMessage.ListenChannel> {

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ListenChannel request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.listenChannel(fromUser, request.getChannelId(), request.getListen()>0);
        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
            WFCMessage.ModifyUserSettingReq modifyUserSettingReq = WFCMessage.ModifyUserSettingReq.newBuilder().setScope(kUserSettingListenedChannels).setKey(request.getChannelId()).setValue(request.getListen() > 0 ? "1" : "0").build();
            mServer.internalRpcMsg(fromUser, null, modifyUserSettingReq.toByteArray(), 0, fromUser, PutUserSettingTopic, false);

            publisher.notifyChannelListenStatusChanged(m_messagesStore.getChannelInfo(request.getChannelId()), fromUser, request.getListen() > 0);
        }
        return errorCode;
    }
}

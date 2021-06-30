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
@Handler(IMTopic.GetChatroomInfoTopic)
public class GetChatroomInfoHandler extends IMHandler<WFCMessage.GetChatroomInfoRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.GetChatroomInfoRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        WFCMessage.ChatroomInfo info = m_messagesStore.getChatroomInfo(request.getChatroomId());
        if (info == null) {
            errorCode = ErrorCode.ERROR_CODE_NOT_EXIST;
        } else if(info.getUpdateDt() <= request.getUpdateDt()) {
            errorCode = ErrorCode.ERROR_CODE_NOT_MODIFIED;
        } else {
            int memberCount = m_messagesStore.getChatroomMemberCount(request.getChatroomId());
            byte[] data = info.toBuilder().setMemberCount(memberCount).build().toByteArray();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}

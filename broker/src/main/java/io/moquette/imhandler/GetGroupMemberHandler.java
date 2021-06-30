/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.ProtoUtil;
import cn.wildfirechat.proto.WFCMessage;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import java.util.ArrayList;
import java.util.List;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import io.moquette.spi.impl.Qos1PublishHandler;
//TODO 获取群聊成员
@Handler(IMTopic.GetGroupMemberTopic)
public class GetGroupMemberHandler extends IMHandler<WFCMessage.PullGroupMemberRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullGroupMemberRequest request, Qos1PublishHandler.IMCallback callback) {
        List<WFCMessage.GroupMember> members = new ArrayList<>();
        ErrorCode errorCode = m_messagesStore.getGroupMembers(request.getTarget(), request.getHead(), members);

        if (errorCode == ERROR_CODE_SUCCESS) {
            WFCMessage.PullGroupMemberResult result = WFCMessage.PullGroupMemberResult.newBuilder().addAllMember(members).build();
            String json = ProtoUtil.toJson(WFCMessage.PullGroupMemberResult.class, result);
            System.out.println(json);
            byte[] data = json.getBytes();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}

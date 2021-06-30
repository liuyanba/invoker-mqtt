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

import java.util.List;
import io.moquette.spi.impl.Qos1PublishHandler;
//TODO 获取群聊详情
@Handler(IMTopic.GetGroupInfoTopic)
public class GetGroupInfoHandler extends IMHandler<WFCMessage.PullUserRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullUserRequest request, Qos1PublishHandler.IMCallback callback) {
        List<WFCMessage.GroupInfo> infos = m_messagesStore.getGroupInfos(request.getRequestList());

        WFCMessage.PullGroupInfoResult result = WFCMessage.PullGroupInfoResult.newBuilder().addAllInfo(infos).build();
        String json = ProtoUtil.toJson(WFCMessage.PullGroupInfoResult.class, result);
        byte[] data = json.getBytes();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}

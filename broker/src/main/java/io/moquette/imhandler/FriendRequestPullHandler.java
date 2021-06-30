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
//TODO 获取好友申请通知
@Handler(IMTopic.FriendRequestPullTopic)
public class FriendRequestPullHandler extends IMHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
        List<WFCMessage.FriendRequest> friendDatas = m_messagesStore.getFriendRequestList(fromUser, request.getVersion());
        WFCMessage.GetFriendRequestResult.Builder builder = WFCMessage.GetFriendRequestResult.newBuilder();
        builder.addAllEntry(friendDatas);
        String jsonResult = ProtoUtil.toJson(WFCMessage.GetFriendRequestResult.class, builder.build());
        System.out.println("FRP jsonResult：" + jsonResult);
        LOG.info("FRP LIU YAN jsonResult： {} " , jsonResult);
        byte[] data = jsonResult.getBytes();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}

/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.ProtoUtil;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.util.StringUtil;
import com.secret.loServer.model.FriendData;
import io.netty.buffer.ByteBuf;
import com.liuyan.im.IMTopic;

import java.util.List;
import io.moquette.spi.impl.Qos1PublishHandler;
//TODO 拉取未添加好友列表


@Handler(IMTopic.FriendPullTopic)
public class FriendPullHandler extends IMHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
        List<FriendData> friendDatas = m_messagesStore.getFriendList(fromUser, clientID, request.getVersion());
        WFCMessage.GetFriendsResult.Builder builder = WFCMessage.GetFriendsResult.newBuilder();
        for (FriendData data : friendDatas) {
            WFCMessage.Friend.Builder builder1 = WFCMessage.Friend.newBuilder().setState(data.getState()).setBlacked(data.getBlacked()).setUid(data.getFriendUid()).setUpdateDt(data.getTimestamp());
            if (!StringUtil.isNullOrEmpty(data.getAlias())) {
                builder1.setAlias(data.getAlias());
            }
            if (!StringUtil.isNullOrEmpty(data.getExtra())) {
                builder1.setExtra(data.getExtra());
            }
            builder.addEntry(builder1.build());
        }
        //构造一个朋友对象
      /*  WFCMessage.Friend friend=WFCMessage.Friend.newBuilder().setState(1).setExtra("123").setUid("12312").setAlias("123123").setUpdateDt(123123L).build();
        builder.addEntry(friend);*/
        byte[] data = builder.build().toByteArray();
        // new Gson().toJson(result)
        String jsonResult = ProtoUtil.toJson(WFCMessage.GetFriendsResult.class, builder.build());
        System.out.println(jsonResult);
        LOG.info("FP LIU YAN Result:{}", jsonResult);
        ackPayload.ensureWritable(data.length).writeBytes(jsonResult.getBytes());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}

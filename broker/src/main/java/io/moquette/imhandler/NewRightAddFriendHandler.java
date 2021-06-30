package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.WFCMessage;
import com.liuyan.im.IMTopic;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;

/**
 * @author Liu_Yan-admin
 * @create 2021/3/16 14:51
 * @description 新版添加好友, 调用后直接添加成功, 但属于单向添加
 * @Version: 1.0
 */
@Handler(IMTopic.NewAddFriendTopic)
public class NewRightAddFriendHandler extends IMHandler<WFCMessage.PullUserRequest> {


    /**
     * @param ackPayload 负责输出流
     * @param clientID   客户端Id
     * @param fromUser   用户id
     * @param isAdmin    是否管理员
     * @param request    请求体
     * @param callback   回调
     * @return ErrorCode
     */
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullUserRequest request, Qos1PublishHandler.IMCallback callback) {
        return null;
    }

}
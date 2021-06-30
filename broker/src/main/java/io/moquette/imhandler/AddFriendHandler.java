/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.SecretHttpUtils;
import cn.secret.util.pojo.cloud.Profile;
import cn.secret.util.pojo.cloud.RestResult;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hazelcast.util.StringUtil;
import io.moquette.BrokerConstants;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.IMTopic;

import java.util.HashMap;
import java.util.Map;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import static com.liuyan.im.IMTopic.HandleFriendRequestTopic;

//TODO 发送好友请求
@Handler(IMTopic.AddFriendRequestTopic)
public class AddFriendHandler extends GroupHandler<WFCMessage.AddFriendRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(AddFriendHandler.class);

    String getProfileUrl = mServer.getConfig().getProperty(BrokerConstants.GET_PROFILE_URL);

    /**
     *
     * @param ackPayload
     * @param clientID 发送者的ClientId
     * @param fromUser 发送者的用户名(userCode)
     * @param isAdmin 是否是管理员(一般都为false)
     * @param request 请求体
     * @param callback 回调数据 里面包含请求的topic  请求者信息
     * @return
     */
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.AddFriendRequest request, Qos1PublishHandler.IMCallback callback) {
        System.out.println("======== FAR request" + request);
        long[] head = new long[1];

        // 这里判断 用户是否存在,如果不存在 则创建一个新用户
        checkUserIsExist(request.getTargetUid());

        //校验是否添加过好友
        ErrorCode errorCode = m_messagesStore.saveAddFriendRequest(fromUser, request, head);
        if (errorCode == ERROR_CODE_SUCCESS) {
            WFCMessage.User user = m_messagesStore.getUserInfo(request.getTargetUid());
            if (user != null && user.getType() == ProtoConstants.UserType.UserType_Normal) {
                publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, request.getTargetUid(), head[0], fromUser, request.getReason());
            } else if (user != null && user.getType() == ProtoConstants.UserType.UserType_Robot) {
                WFCMessage.HandleFriendRequest handleFriendRequest = WFCMessage.HandleFriendRequest.newBuilder().setTargetUid(fromUser).setStatus(ProtoConstants.FriendRequestStatus.RequestStatus_Accepted).build();
                mServer.internalRpcMsg(request.getTargetUid(), null, handleFriendRequest.toByteArray(), 0, fromUser, HandleFriendRequestTopic, false);
            }
        }
        Map resultMap = new HashMap();
        resultMap.put("code", errorCode);
        String resultjson = JSONObject.toJSON(resultMap).toString();
        LOG.info("FAR LIU YAN Result:{}", resultjson);
        ackPayload.writeBytes(resultjson.getBytes());
        return ERROR_CODE_SUCCESS;
    }

    private void checkUserIsExist(String userCode) {
    }


    @Override
    public ErrorCode receiveAction(ByteBuf ackPayload, String clientID, String
            fromUser, WFCMessage.AddFriendRequest request) {
        ErrorCode errorCode = ERROR_CODE_SUCCESS;
        WFCMessage.User user = m_messagesStore.getUserInfo(request.getTargetUid());
        if (user != null && user.getType() == ProtoConstants.UserType.UserType_Normal) {
            publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, request.getTargetUid(), System.currentTimeMillis(), fromUser, request.getReason());
        } else if (user != null && user.getType() == ProtoConstants.UserType.UserType_Robot) {
            WFCMessage.HandleFriendRequest handleFriendRequest = WFCMessage.HandleFriendRequest.newBuilder().setTargetUid(fromUser).setStatus(ProtoConstants.FriendRequestStatus.RequestStatus_Accepted).build();
            mServer.internalRpcMsg(request.getTargetUid(), null, handleFriendRequest.toByteArray(), 0, fromUser, HandleFriendRequestTopic, false);
        }
        return errorCode;
    }
}

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

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import io.moquette.spi.impl.Qos1PublishHandler;
//TODO 获取未读好友列表
@Handler(IMTopic.RriendRequestUnreadSyncTopic)
public class SyncFriendRequestUnreadHandler extends GroupHandler<WFCMessage.Version> {

    /**
     *
     * @param ackPayload
     * @param clientID 发送消息者的 clientID
     * @param fromUser 发送消息者的 用户名
     * @param isAdmin  一般是false
     * @param request  请求体
     * @param callback
     * @return
     */
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
        long[] head = new long[1];
        ErrorCode errorCode = m_messagesStore.SyncFriendRequestUnread(fromUser, request.getVersion(), head);
        if (errorCode == ERROR_CODE_SUCCESS) {
            // publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, fromUser, head[0]);
        }
        return errorCode;
    }

    @Override
    public ErrorCode receiveAction(ByteBuf ackPayload, String clientID, String fromUser, WFCMessage.Version request) {
        publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, fromUser, System.currentTimeMillis());
        return ERROR_CODE_SUCCESS;
    }
}

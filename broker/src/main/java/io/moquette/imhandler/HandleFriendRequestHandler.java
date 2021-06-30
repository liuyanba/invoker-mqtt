/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.I18n;
import com.liuyan.im.IMTopic;
import com.liuyan.im.MessageShardingUtil;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

//TODO 同意好友申请
@Handler(IMTopic.HandleFriendRequestTopic)
public class HandleFriendRequestHandler extends IMHandler<WFCMessage.HandleFriendRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(HandleFriendRequestHandler.class);

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.HandleFriendRequest request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
        builder.setFromUser(request.getTargetUid());
        long[] heads = new long[2];
        ErrorCode errorCode = m_messagesStore.handleFriendRequest(fromUser, request, builder, heads, isAdmin);

        if (errorCode == ERROR_CODE_SUCCESS) {
            if (!isAdmin && builder.getConversation() != null && request.getStatus() == ProtoConstants.FriendRequestStatus.RequestStatus_Accepted) {
                long messageId = MessageShardingUtil.generateId();
                long timestamp = System.currentTimeMillis();
                builder.setMessageId(messageId);
                builder.setServerTimestamp(timestamp);
                //saveAndPublish(request.getTargetUid(), null, builder.build(), false);


               /*
               MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
               if (session != null && !StringUtil.isNullOrEmpty(session.getLanguage())) {
                    language = session.getLanguage();
                }*/

                String language = "zh_CN";
                LOG.debug(" Above_Greeting_Message :" + I18n.getString(language, "Above_Greeting_Message"));
                System.out.println(" Above_Greeting_Message :" + I18n.getString(language, "Above_Greeting_Message"));
                WFCMessage.MessageContent.Builder contentBuilder = WFCMessage.MessageContent.newBuilder().setType(90).setContent("以上是打招呼信息");


                builder = WFCMessage.Message.newBuilder();
                builder.setFromUser(fromUser);
                builder.setConversation(WFCMessage.Conversation.newBuilder().setTarget(request.getTargetUid()).setLine(0).setType(ProtoConstants.ConversationType.ConversationType_Private).build());
                builder.setContent(contentBuilder);
                timestamp = System.currentTimeMillis();
                builder.setServerTimestamp(timestamp);

                messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                //  saveAndPublish(request.getTargetUid(), null, builder.build(), false);
                LOG.debug(" Friend_Can_Start_Chat :" + I18n.getString(language, "Friend_Can_Start_Chat"));
                System.out.println("language:" + language + " Friend_Can_Start_Chat :" + I18n.getString(language, "Friend_Can_Start_Chat"));
                contentBuilder.setContent("我们已经成为好友了，现在可以开始聊天");
                builder.setContent(contentBuilder);
                messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                timestamp = System.currentTimeMillis();
                builder.setServerTimestamp(timestamp);
                saveAndPublish(fromUser, null, builder.build(), false);

                publisher.publishNotification(IMTopic.NotifyFriendTopic, request.getTargetUid(), heads[0]);
                publisher.publishNotification(IMTopic.NotifyFriendTopic, fromUser, heads[1]);
            } else if (!isAdmin && builder.getConversation() != null && request.getStatus() == ProtoConstants.FriendRequestStatus.RequestStatus_Rejected) {

                long messageId = MessageShardingUtil.generateId();
                long timestamp = System.currentTimeMillis();
                builder.setMessageId(messageId);
                builder.setServerTimestamp(timestamp);
                WFCMessage.MessageContent.Builder contentBuilder = WFCMessage.MessageContent.newBuilder().setType(91).setContent("对方拒绝了您的好友申请");
                builder = WFCMessage.Message.newBuilder();
                builder.setFromUser(request.getTargetUid());
                builder.setConversation(WFCMessage.Conversation.newBuilder().setTarget(fromUser).setLine(0).setType(ProtoConstants.ConversationType.ConversationType_Private).build());
                builder.setContent(contentBuilder);
                timestamp = System.currentTimeMillis();
                builder.setServerTimestamp(timestamp);

                messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                // saveAndPublish(request.getTargetUid(), null, builder.build(), false);

                contentBuilder.setContent("对方拒绝了您的好友申请");
                builder.setContent(contentBuilder);
                messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                timestamp = System.currentTimeMillis();
                builder.setServerTimestamp(timestamp);
                saveAndPublish(request.getTargetUid(), null, builder.build(), false);

                publisher.publishNotification(IMTopic.NotifyFriendTopic, request.getTargetUid(), heads[0]);

            }
        }
        return errorCode;
    }
}

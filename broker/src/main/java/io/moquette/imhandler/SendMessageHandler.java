/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.SecretHttpUtils;
import cn.secret.util.ProtoUtil;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hazelcast.util.StringUtil;
import io.moquette.BrokerConstants;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.IMTopic;
import com.liuyan.im.Utility;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Handler(value = IMTopic.SendMessageTopic)
public class SendMessageHandler extends IMHandler<WFCMessage.Message> {

    private static final Logger LOG = LoggerFactory.getLogger(SendMessageHandler.class);
    private static final String bigDataLable = "表情";

    private int mSensitiveType = 0;  //命中敏感词时，0 失败，1 吞掉， 2 敏感词替换成*。
    private String mForwardUrl = null;
    private String mNotifyUrl = null;
    private String textReviewUrl = null;
    private int mBlacklistStrategy = 0; //黑名单中时，0失败，1吞掉。

    public SendMessageHandler() {
        super();

        String forwardUrl = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Forward_Url);
        if (!StringUtil.isNullOrEmpty(forwardUrl)) {
            mForwardUrl = forwardUrl;
        }
        String notifyUrl = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_NOTIFY_Url);
        if (!StringUtil.isNullOrEmpty(notifyUrl)) {
            mNotifyUrl = notifyUrl;
        }
        String reviewUrl = mServer.getConfig().getProperty(BrokerConstants.TEXT_REVIEW_URL);
        if (!StringUtil.isNullOrEmpty(reviewUrl)) {
            textReviewUrl = reviewUrl;
        }

        try {
            mSensitiveType = Integer.parseInt(mServer.getConfig().getProperty(BrokerConstants.SENSITIVE_Filter_Type));
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

        try {
            mBlacklistStrategy = Integer.parseInt(mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Blacklist_Strategy));
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }
    //大数据文本审核替换im文本审核
              /*  Set<String> matched = m_messagesStore.handleSensitiveWord(message.getContent().getSearchableContent());
                if (matched != null && !matched.isEmpty()) {
                    m_messagesStore.storeSensitiveMessage(message);
                    if (mSensitiveType == 0) {
                        errorCode = ErrorCode.ERROR_CODE_SENSITIVE_MATCHED;
                    } else if (mSensitiveType == 1) {
                        ignoreMsg = true;
                    } else if (mSensitiveType == 2) {
                        String text = message.getContent().getSearchableContent();
                        for (String word : matched) {
                            text = text.replace(word, "***");
                        }

                        message = message.toBuilder().setContent(message.getContent().toBuilder().setSearchableContent(text).build()).build();
                    } else if (mSensitiveType == 3) {

                    }
                }*/

    /**
     * @param ackPayload
     * @param clientID
     * @param fromUser
     * @param isAdmin
     * @param message
     * @param callback
     * @return
     */
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Message message, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        if (message != null) {
            boolean ignoreMsg = false;
            if (!isAdmin) {  //admin do not check the right
                // 不能在端上直接发送撤回和群通知
                if (message.getContent().getType() == 80 || (message.getContent().getType() >= 100 && message.getContent().getType() < 200)) {
                    return ErrorCode.INVALID_PARAMETER;
                }
                int userStatus = m_messagesStore.getUserStatus(fromUser);
                if (userStatus == ProtoConstants.UserStatus.Muted || userStatus == ProtoConstants.UserStatus.Forbidden) {
                    return ErrorCode.ERROR_CODE_FORBIDDEN_SEND_MSG;
                }
                //TODO 私信发送
                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                    errorCode = m_messagesStore.isAllowUserMessage(message.getConversation().getTarget(), fromUser);
                    if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                        if (errorCode == ErrorCode.ERROR_CODE_IN_BLACK_LIST && mBlacklistStrategy != ProtoConstants.BlacklistStrategy.Message_Reject) {
                            ignoreMsg = true;
                            errorCode = ErrorCode.ERROR_CODE_SUCCESS;
                        } else {
                            return errorCode;
                        }
                    }

                    userStatus = m_messagesStore.getUserStatus(message.getConversation().getTarget());
                    if (userStatus == ProtoConstants.UserStatus.Forbidden) {
                        return ErrorCode.ERROR_CODE_USER_FORBIDDEN;
                    }
                }

                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group) {
                    errorCode = m_messagesStore.canSendMessageInGroup(fromUser, message.getConversation().getTarget());
                    if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                        return errorCode;
                    }
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_ChatRoom) {
                    if (!m_messagesStore.checkUserClientInChatroom(fromUser, clientID, message.getConversation().getTarget())) {
                        return ErrorCode.ERROR_CODE_NOT_IN_CHATROOM;
                    }
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel) {
                    if (!m_messagesStore.checkUserInChannel(fromUser, message.getConversation().getTarget())) {
                        return ErrorCode.ERROR_CODE_NOT_IN_CHANNEL;
                    }
                }
            }

            if (mForwardUrl != null) {
                publisher.forwardMessage(message, mForwardUrl);
            }


            String messageContent = message.getContent().getSearchableContent();
            Map<String, String> map = new HashMap<>();
            map.put("text", messageContent);
            //大数据文本审核替换im文本审核
            if (ProtoConstants.ContentType.Text == message.getContent().getType()) {
                //只处理文本消息   type==1的
                try {
                    LOG.info("审核前内容:{}", message.getContent().getSearchableContent());
                    String resultJson = SecretHttpUtils.post(textReviewUrl, map, "UTF-8");
                    LOG.info("大数据返回文本审核结果 ： " + resultJson + "   地址:" + textReviewUrl);
                    if (!StringUtil.isNullOrEmptyAfterTrim(resultJson)) {
                    }
                } catch (UnsupportedEncodingException e) {
                    LOG.error("大数据文本审核异常", e);
                    e.printStackTrace();
                }
            }

            WFCMessage.MessageContent.Builder contentBuilder = WFCMessage.MessageContent.newBuilder()
                    .setContent(messageContent)
                    .setSearchableContent(messageContent)
                    .setType(message.getContent().getType())
                    .setPersistFlag(message.getContent().getPersistFlag());
            message = message.toBuilder().setContent(contentBuilder).build();


            //落库与推送
            saveAndPublish(fromUser, clientID, message, ignoreMsg);

            Map resultMap = new HashMap();
            resultMap.put("code", errorCode);
            ackPayload = ackPayload.capacity(20);
            String jsonMessage = ProtoUtil.toJson(WFCMessage.Message.class, message);
            resultMap.put("msg", jsonMessage);

            ackPayload.writeLong(message.getMessageId());
            ackPayload.writeLong(message.getServerTimestamp());
            String resultjson = JSONObject.toJSON(resultMap).toString();
            System.out.println("MS writeBytes:" + resultjson);
            LOG.info("MS  writeBytes： " + resultjson);
            ackPayload.writeBytes(resultjson.getBytes());
        } else {
            Map resultMap = new HashMap();
            resultMap.put("code", ErrorCode.ERROR_CODE_INVALID_MESSAGE);
            String resultjson = JSONObject.toJSON(resultMap).toString();
            ackPayload.writeBytes(resultjson.getBytes());
            errorCode = ErrorCode.ERROR_CODE_INVALID_MESSAGE;
        }
        return errorCode;
    }

}

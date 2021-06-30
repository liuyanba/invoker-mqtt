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


@Handler(value = IMTopic.SendMessageToClusterTopic)
public class SendMessageToClusterHandler extends IMHandler<WFCMessage.Message> {

    private static final Logger LOG = LoggerFactory.getLogger(SendMessageToClusterHandler.class);
    private static final String bigDataLable = "表情";

    private int mSensitiveType = 0;  //命中敏感词时，0 失败，1 吞掉， 2 敏感词替换成*。
    private String textReviewUrl = null;
    private int mBlacklistStrategy = 0; //黑名单中时，0失败，1吞掉。

    public SendMessageToClusterHandler() {
        super();

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
            String messageContent = message.getContent().getSearchableContent();
            Map<String, String> map = new HashMap<>();
            map.put("text", messageContent);
            //大数据文本审核替换im文本审核
            if (ProtoConstants.ContentType.Text == message.getContent().getType()) {
                try {
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

            Map resultMap = new HashMap();
            resultMap.put("code", errorCode);

            ackPayload = ackPayload.capacity(20);
            String jsonMessage = ProtoUtil.toJson(WFCMessage.Message.class, message);
            resultMap.put("msg", jsonMessage);
            ackPayload.writeLong(message.getMessageId());
            ackPayload.writeLong(message.getServerTimestamp());
            String resultjson = JSONObject.toJSON(resultMap).toString();
            LOG.info("MS Cluster writeBytes： " + resultjson);
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

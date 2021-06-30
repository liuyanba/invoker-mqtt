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
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(IMTopic.GetUserInfoTopic)
public class GetUserInfoHandler extends IMHandler<WFCMessage.PullUserRequest> {


    private static final Logger log = LoggerFactory.getLogger(GetUserInfoHandler.class);

    /**
     *
     * @param ackPayload
     * @param clientID 客户端id
     * @param fromUser 发送者UserCode
     * @param isAdmin 是否是管理员 一般为false
     * @param request 请求体
     * @param callback 回调信息 包含发送者信息和请求的topic等
     * @return
     */
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullUserRequest request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.PullUserResult.Builder resultBuilder = WFCMessage.PullUserResult.newBuilder();

        ErrorCode errorCode = m_messagesStore.getUserInfo(request.getRequestList(), resultBuilder);
        if (errorCode == ERROR_CODE_SUCCESS) {
            String json = ProtoUtil.toJson(WFCMessage.PullUserResult.class, resultBuilder.build());
            System.out.println(json);
            log.info("UPUI LIU YAN JsonResult:{}", json);
            byte[] data = json.getBytes();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}

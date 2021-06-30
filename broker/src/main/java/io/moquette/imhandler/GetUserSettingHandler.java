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

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(IMTopic.GetUserSettingTopic)
public class GetUserSettingHandler extends IMHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.GetUserSettingResult.Builder builder = WFCMessage.GetUserSettingResult.newBuilder();
        ErrorCode errorCode = m_messagesStore.getUserSettings(fromUser, request.getVersion(), builder);
        if (errorCode == ERROR_CODE_SUCCESS) {
            String json = ProtoUtil.toJson(WFCMessage.GetUserSettingResult.class, builder.build());
            LOG.debug("GetUserSettingTopic result:" + json);
            LOG.info("UG LIU YAN Result:{}", json);
            byte[] data = json.getBytes();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}

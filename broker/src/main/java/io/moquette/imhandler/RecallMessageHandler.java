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
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(value = IMTopic.RecallMessageTopic)
public class RecallMessageHandler extends IMHandler<WFCMessage.INT64Buf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.INT64Buf int64Buf, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.recallMessage(int64Buf.getId(), fromUser, clientID, isAdmin);

        if(errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
            return errorCode;
        }

        WFCMessage.Message message = m_messagesStore.getMessage(int64Buf.getId());
        if (message == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        publish(fromUser, clientID, message);

        return errorCode;
    }

}

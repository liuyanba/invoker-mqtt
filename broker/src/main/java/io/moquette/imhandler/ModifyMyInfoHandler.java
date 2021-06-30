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
import com.liuyan.im.Utility;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(IMTopic.ModifyMyInfoTopic)
public class ModifyMyInfoHandler extends IMHandler<WFCMessage.ModifyMyInfoRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyMyInfoRequest request, Qos1PublishHandler.IMCallback callback) {
        try {
            return m_messagesStore.modifyUserInfo(fromUser, request);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
            return ErrorCode.ERROR_CODE_SERVER_ERROR;
        }
    }
}

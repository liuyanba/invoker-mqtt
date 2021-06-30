/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.moquette.spi.impl.security.TokenAuthenticator;
import io.netty.buffer.ByteBuf;
import com.liuyan.im.IMTopic;

@Handler(IMTopic.GetTokenTopic)
public class GetTokenHandler extends IMHandler<WFCMessage.GetTokenRequest> {


    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.GetTokenRequest request, Qos1PublishHandler.IMCallback callback) {
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}

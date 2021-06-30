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
import com.hazelcast.util.StringUtil;
import io.netty.buffer.ByteBuf;

import static com.liuyan.im.IMTopic.KickoffPCClientTopic;
import io.moquette.spi.impl.Qos1PublishHandler;
@Handler(value = KickoffPCClientTopic)
public class KickoffPCClientHandler extends GroupHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        String pcClientId = request.getId();
        if (StringUtil.isNullOrEmpty(pcClientId)) {
            return ErrorCode.INVALID_PARAMETER;
        }

        return m_sessionsStore.kickoffPCClient(fromUser, pcClientId);
    }
}

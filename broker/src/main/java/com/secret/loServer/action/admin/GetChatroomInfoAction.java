/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;

@Route(APIPath.Chatroom_Info)
@HttpMethod("POST")
public class GetChatroomInfoAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetChatroomInfo getChatroomInfo = getRequestBody(request.getNettyRequest(), InputGetChatroomInfo.class);
            String chatroomid = getChatroomInfo.getChatroomId();
            if (!StringUtil.isNullOrEmpty(chatroomid)) {

                WFCMessage.ChatroomInfo info = messagesStore.getChatroomInfo(chatroomid);

                RestResult result;
                if (info != null) {
                    result = RestResult.ok(new OutputGetChatroomInfo(chatroomid, messagesStore.getChatroomMemberCount(chatroomid), info));
                } else {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                }
                response.setStatus(HttpResponseStatus.OK);

                response.setContent(new Gson().toJson(result));

            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }

        }
        return true;
    }
}

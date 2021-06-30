/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import cn.wildfirechat.pojos.InputDestoryChatroom;
import io.moquette.spi.impl.Utils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;

@Route(APIPath.Chatroom_Destroy)
@HttpMethod("POST")
public class DestoryChatroomAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest)request.getNettyRequest();
            byte[] bytes = Utils.readBytesAndRewind(fullHttpRequest.content());
            String content = new String(bytes);
            Gson gson = new Gson();
            InputDestoryChatroom inputDestoryChatroom = gson.fromJson(content, InputDestoryChatroom.class);
            if (inputDestoryChatroom != null
                && !StringUtil.isNullOrEmpty(inputDestoryChatroom.getChatroomId())) {

                messagesStore.destoryChatroom(inputDestoryChatroom.getChatroomId());

                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.ok();
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

/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputGetChatroomInfo;
import cn.wildfirechat.pojos.OutputStringList;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.moquette.persistence.UserClientEntry;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Route(APIPath.Chatroom_GetMembers)
@HttpMethod("POST")
public class GetChatroomMembersAction extends AdminAction {

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

                Collection<UserClientEntry> members = messagesStore.getChatroomMembers(chatroomid);

                List<String> list = new ArrayList<>();
                for (UserClientEntry entry : members) {
                    list.add(entry.userId);
                }

                RestResult result;
                if (!list.isEmpty()) {
                    result = RestResult.ok(new OutputStringList(list));
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

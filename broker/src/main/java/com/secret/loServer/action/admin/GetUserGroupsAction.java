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
import cn.wildfirechat.pojos.InputUserId;
import cn.wildfirechat.pojos.OutputGroupIds;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Set;

@Route(APIPath.Get_User_Groups)
@HttpMethod("POST")
public class GetUserGroupsAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUserId inputUserId = getRequestBody(request.getNettyRequest(), InputUserId.class);
            if (inputUserId != null
                && (!StringUtil.isNullOrEmpty(inputUserId.getUserId()))) {

                Set<String> groupIds = messagesStore.getUserGroupIds(inputUserId.getUserId());
                response.setStatus(HttpResponseStatus.OK);
                OutputGroupIds outputGroupIds = new OutputGroupIds();
                outputGroupIds.setGroupIds(new ArrayList<>(groupIds));
                RestResult result = RestResult.ok(outputGroupIds);

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

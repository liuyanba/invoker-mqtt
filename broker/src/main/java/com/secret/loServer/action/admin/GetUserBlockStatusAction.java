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
import cn.wildfirechat.pojos.InputGetUserInfo;
import cn.wildfirechat.pojos.OutputUserStatus;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;

@Route(APIPath.User_Check_Block_Status)
@HttpMethod("POST")
public class GetUserBlockStatusAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetUserInfo inputUserId = getRequestBody(request.getNettyRequest(), InputGetUserInfo.class);
            if (inputUserId != null
                && !StringUtil.isNullOrEmpty(inputUserId.getUserId())) {

                int status = messagesStore.getUserStatus(inputUserId.getUserId());
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.ok(new OutputUserStatus(status));
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

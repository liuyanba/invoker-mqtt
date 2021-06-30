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
import cn.wildfirechat.pojos.SystemSettingPojo;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Put_System_Setting)
@HttpMethod("POST")
public class PutSystemSettingAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            SystemSettingPojo inputUserId = getRequestBody(request.getNettyRequest(), SystemSettingPojo.class);
            if (inputUserId != null
                && (!StringUtil.isNullOrEmpty(inputUserId.value) || !StringUtil.isNullOrEmpty(inputUserId.desc))) {


                boolean success = messagesStore.updateSystemSetting(inputUserId.id, inputUserId.value, inputUserId.desc);

                response.setStatus(HttpResponseStatus.OK);
                RestResult result;
                if (!success) {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_SERVER_ERROR);
                } else {
                    result = RestResult.ok();
                }

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

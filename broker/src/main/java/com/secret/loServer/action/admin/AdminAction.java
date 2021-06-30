/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.action.Action;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.netty.handler.codec.http.HttpResponseStatus;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.LoggerFactory;
import com.liuyan.im.RateLimiter;

abstract public class AdminAction extends Action {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AdminAction.class);
    private static String SECRET_KEY = "123456";
    private static boolean NO_CHECK_TIME = false;
    private final RateLimiter mLimitCounter = new RateLimiter(10, 500);

    public static void setSecretKey(String secretKey) {
        SECRET_KEY = secretKey;
    }

    public static void setNoCheckTime(String noCheckTime) {
        try {
            NO_CHECK_TIME = Boolean.parseBoolean(noCheckTime);
        } catch (Exception e) {

        }
    }

    @Override
    public ErrorCode preAction(Request request, Response response) {
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    protected void sendResponse(Response response, ErrorCode errorCode, Object data) {
        response.setStatus(HttpResponseStatus.OK);
        if (errorCode == null) {
            errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        }

        RestResult result = RestResult.resultOf(errorCode, errorCode.getMsg(), data);
        response.setContent(new Gson().toJson(result));
        response.send();
    }
}

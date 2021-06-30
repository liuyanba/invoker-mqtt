/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.robot;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.action.Action;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.moquette.spi.impl.Utils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.RateLimiter;
import com.liuyan.im.Utility;

import java.io.UnsupportedEncodingException;

abstract public class RobotAction extends Action {
    protected static final Logger LOG = LoggerFactory.getLogger(RobotAction.class);
    private final RateLimiter mLimitCounter = new RateLimiter(10, 1000);

    protected WFCMessage.Robot robot;

    @Override
    public ErrorCode preAction(Request request, Response response) {
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String rid = request.getHeader("rid");
        if (StringUtil.isNullOrEmpty(nonce) || StringUtil.isNullOrEmpty(timestamp) || StringUtil.isNullOrEmpty(sign) || StringUtil.isNullOrEmpty(rid)) {
            return ErrorCode.INVALID_PARAMETER;
        }

        if (!mLimitCounter.isGranted(rid)) {
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }

        Long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
            return ErrorCode.INVALID_PARAMETER;
        }

        if (System.currentTimeMillis() - ts > 2 * 60 * 60 * 1000) {
            return ErrorCode.ERROR_CODE_SIGN_EXPIRED;
        }

        robot = messagesStore.getRobot(rid);
        if (robot == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        String str = nonce + "|" + robot.getSecret() + "|" + timestamp;
        String localSign = DigestUtils.sha1Hex(str);
        return localSign.equals(sign) ? ErrorCode.ERROR_CODE_SUCCESS : ErrorCode.ERROR_CODE_AUTH_FAILURE;
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

    protected <T> T getRequestBody(HttpRequest request, Class<T> cls) {
        if (request instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            byte[] bytes = Utils.readBytesAndRewind(fullHttpRequest.content());
            String content = null;
            try {
                content = new String(bytes, "UTF-8");
                Gson gson = new Gson();
                T t = gson.fromJson(content, cls);
                return t;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        }
        return null;
    }
}

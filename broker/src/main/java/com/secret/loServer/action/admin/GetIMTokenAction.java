/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.core.IMap;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import cn.wildfirechat.pojos.InputGetToken;
import cn.wildfirechat.pojos.OutputGetIMTokenData;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.moquette.server.Server;
import io.netty.handler.codec.http.FullHttpRequest;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import java.util.Base64;
import java.util.concurrent.Executor;

@Route(APIPath.User_Get_Token)
@HttpMethod("POST")
public class GetIMTokenAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return false;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetToken input = getRequestBody(request.getNettyRequest(), InputGetToken.class);
            String userId = input.getUserId();


            WFCMessage.GetTokenRequest getTokenRequest = WFCMessage.GetTokenRequest.newBuilder().setUserId(userId).setClientId(input.getClientId()).setPlatform(input.getPlatform() == null ? 0 : input.getPlatform()).build();
            RPCCenter.getInstance().sendRequest(userId, input.getClientId(), IMTopic.GetTokenTopic, getTokenRequest.toByteArray(), userId, TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
                @Override
                public void onSuccess(byte[] result) {
                    ErrorCode errorCode1 = ErrorCode.fromCode(result[0]);
                    if (errorCode1 == ErrorCode.ERROR_CODE_SUCCESS) {
                        //ba errorcode qudiao
                        byte[] data = new byte[result.length - 1];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = result[i + 1];
                        }
                        String token = Base64.getEncoder().encodeToString(data);
                        sendResponse(response, null, new OutputGetIMTokenData(userId, token));
                    } else {
                        sendResponse(response, errorCode1, null);
                    }
                }

                @Override
                public void onError(ErrorCode errorCode) {
                    sendResponse(response, errorCode, null);
                }

                @Override
                public void onTimeout() {
                    sendResponse(response, ErrorCode.ERROR_CODE_TIMEOUT, null);
                }

                @Override
                public Executor getResponseExecutor() {
                    return command -> {
                        ctx.executor().execute(command);
                    };
                }
            }, true);
            return false;
        }
        return true;
    }
}

/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;


import cn.wildfirechat.common.APIPath;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import cn.wildfirechat.pojos.InputAddGroupMember;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import cn.wildfirechat.common.ErrorCode;
import com.liuyan.im.IMTopic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Route(APIPath.Group_Member_Add)
@HttpMethod("POST")
public class AddGroupMemberAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputAddGroupMember inputAddGroupMember = getRequestBody(request.getNettyRequest(), InputAddGroupMember.class);
            Map map = new HashMap();
            map.put("groupId", inputAddGroupMember.getGroup_id());
            map.put("addedMember", inputAddGroupMember.getMembers());
            map.put("toLine", inputAddGroupMember.getTo_lines());

            String json = JSONObject.toJSON(map).toString();
            System.out.println(json);
            if (inputAddGroupMember.isValide()) {
                RPCCenter.getInstance().sendRequest(inputAddGroupMember.getOperator(), null, IMTopic.AddGroupMemberTopic, json.getBytes(), inputAddGroupMember.getOperator(), TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
                    @Override
                    public void onSuccess(byte[] result) {
                        ByteBuf byteBuf = Unpooled.buffer();
                        byteBuf.writeBytes(result);
                        ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                            sendResponse(response, null, null);
                        } else {
                            sendResponse(response, errorCode, null);
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
            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }
        }
        return true;
    }
}

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
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import cn.wildfirechat.pojos.InputCreateRobot;
import cn.wildfirechat.pojos.OutputCreateRobot;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.UUIDGenerator;
import com.liuyan.im.Utility;

@Route(APIPath.Create_Robot)
@HttpMethod("POST")
public class CreateRobotAction extends AdminAction {
    private static final Logger LOG = LoggerFactory.getLogger(CreateRobotAction.class);

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputCreateRobot inputCreateRobot = getRequestBody(request.getNettyRequest(), InputCreateRobot.class);
            if (inputCreateRobot != null
                && !StringUtil.isNullOrEmpty(inputCreateRobot.getName())) {

                if(StringUtil.isNullOrEmpty(inputCreateRobot.getPassword())) {
                    inputCreateRobot.setPassword(UUIDGenerator.getUUID());
                }

                if(StringUtil.isNullOrEmpty(inputCreateRobot.getUserId())) {
                    inputCreateRobot.setUserId(messagesStore.getShortUUID());
                }

                if (inputCreateRobot.getPortrait() == null || inputCreateRobot.getPortrait().length() == 0) {
                    inputCreateRobot.setPortrait("https://avatars.io/gravatar/" + inputCreateRobot.getUserId());
                }

                WFCMessage.User newUser = inputCreateRobot.toUser();


                try {
                    messagesStore.addUserInfo(newUser, inputCreateRobot.getPassword());
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    response.setStatus(HttpResponseStatus.OK);
                    RestResult result = RestResult.resultOf(ErrorCode.ERROR_CODE_SERVER_ERROR, e.getMessage());
                    response.setContent(new Gson().toJson(result));
                    return true;
                }

                if (StringUtil.isNullOrEmpty(inputCreateRobot.getOwner())) {
                    inputCreateRobot.setOwner(inputCreateRobot.getUserId());
                }

                if (StringUtil.isNullOrEmpty(inputCreateRobot.getSecret())) {
                    inputCreateRobot.setSecret(UUIDGenerator.getUUID());
                }

                messagesStore.addRobot(inputCreateRobot.toRobot());

                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.ok(new OutputCreateRobot(inputCreateRobot.getUserId(), inputCreateRobot.getSecret()));
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

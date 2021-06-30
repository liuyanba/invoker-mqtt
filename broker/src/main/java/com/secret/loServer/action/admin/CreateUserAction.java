/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.OutputCreateUser;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.UUIDGenerator;
import com.liuyan.im.Utility;

@Route(APIPath.Create_User)
@HttpMethod("POST")
public class CreateUserAction extends AdminAction {
    private static final Logger LOG = LoggerFactory.getLogger(CreateUserAction.class);

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputOutputUserInfo inputCreateUser = getRequestBody(request.getNettyRequest(), InputOutputUserInfo.class);
            if (inputCreateUser != null
                && !StringUtil.isNullOrEmpty(inputCreateUser.getName())
                && (inputCreateUser.getType() == ProtoConstants.UserType.UserType_Normal || inputCreateUser.getType() == ProtoConstants.UserType.UserType_Admin || inputCreateUser.getType() == ProtoConstants.UserType.UserType_Super_Admin)) {

                if(StringUtil.isNullOrEmpty(inputCreateUser.getPassword())) {
                    inputCreateUser.setPassword(UUIDGenerator.getUUID());
                }

                if(StringUtil.isNullOrEmpty(inputCreateUser.getUserId())) {
                    inputCreateUser.setUserId(messagesStore.getShortUUID());
                }

                WFCMessage.User.Builder newUserBuilder = WFCMessage.User.newBuilder()
                    .setUid(StringUtil.isNullOrEmpty(inputCreateUser.getUserId()) ? "" : inputCreateUser.getUserId());
                if (inputCreateUser.getName() != null)
                    newUserBuilder.setName(inputCreateUser.getName());
                if (inputCreateUser.getDisplayName() != null)
                    newUserBuilder.setDisplayName(StringUtil.isNullOrEmpty(inputCreateUser.getDisplayName()) ? inputCreateUser.getName() : inputCreateUser.getDisplayName());
                if (inputCreateUser.getPortrait() != null)
                    newUserBuilder.setPortrait(inputCreateUser.getPortrait());
                if (inputCreateUser.getEmail() != null)
                    newUserBuilder.setEmail(inputCreateUser.getEmail());
                if (inputCreateUser.getAddress() != null)
                    newUserBuilder.setAddress(inputCreateUser.getAddress());
                if (inputCreateUser.getCompany() != null)
                    newUserBuilder.setCompany(inputCreateUser.getCompany());

                if (inputCreateUser.getSocial() != null)
                    newUserBuilder.setSocial(inputCreateUser.getSocial());


                if (inputCreateUser.getMobile() != null)
                    newUserBuilder.setMobile(inputCreateUser.getMobile());
                newUserBuilder.setGender(inputCreateUser.getGender());
                if (inputCreateUser.getExtra() != null)
                    newUserBuilder.setExtra(inputCreateUser.getExtra());

                newUserBuilder.setType(inputCreateUser.getType());
                newUserBuilder.setUpdateDt(System.currentTimeMillis());


                try {
                    messagesStore.addUserInfo(newUserBuilder.build(), inputCreateUser.getPassword());
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    response.setStatus(HttpResponseStatus.OK);
                    RestResult result = RestResult.resultOf(ErrorCode.ERROR_CODE_SERVER_ERROR, e.getMessage());
                    response.setContent(new Gson().toJson(result));
                    return true;
                }

                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.ok(new OutputCreateUser(inputCreateUser.getUserId(), inputCreateUser.getName()));
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

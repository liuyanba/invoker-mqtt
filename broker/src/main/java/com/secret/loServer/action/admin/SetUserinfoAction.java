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
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.internal.LinkedTreeMap;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.moquette.imhandler.IMHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import com.liuyan.im.IMTopic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Route(APIPath.Set_User_Info)
@HttpMethod("POST")
public class SetUserinfoAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            HashMap<String, Object> param = getRequestBody(request.getNettyRequest(), HashMap.class);
            if (param != null) {
                List<WFCMessage.InfoEntry> list = new ArrayList<>();
                try {
                    String userId = (String) param.get("userId");
                    List<LinkedTreeMap> infoEntryList = (List<LinkedTreeMap>) param.get("infoEntry");
                    for (LinkedTreeMap infoEntryMap : infoEntryList) {
                        Double type = (Double) infoEntryMap.get("type");
                        String value = (String) infoEntryMap.get("value");
                        cn.wildfirechat.proto.WFCMessage.InfoEntry infoEntry = cn.wildfirechat.proto.WFCMessage.InfoEntry.newBuilder().setType(type.intValue()).setValue(value).build();
                        list.add(infoEntry);
                    }
                    if (list.size() > 0) {
                        WFCMessage.ModifyMyInfoRequest modifyMyInfoRequest = WFCMessage.ModifyMyInfoRequest.newBuilder().addAllEntry(list).build();
                        ErrorCode errorCode = messagesStore.modifyUserInfo(userId, modifyMyInfoRequest);
                        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                            long updateDt = System.currentTimeMillis();
                            IMHandler.getPublisher().publishNotification(IMTopic.NotifyUserSettingTopic, userId, updateDt);
                            response.setStatus(HttpResponseStatus.OK);
                        } else {
                            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }
}

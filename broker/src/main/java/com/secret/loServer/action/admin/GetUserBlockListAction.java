/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.OutputUserBlockStatusList;
import com.google.gson.Gson;
import com.secret.loServer.RestResult;
import com.secret.loServer.annotation.HttpMethod;
import com.secret.loServer.annotation.Route;
import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

@Route(APIPath.User_Get_Blocked_List)
@HttpMethod("POST")
public class GetUserBlockListAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            response.setStatus(HttpResponseStatus.OK);
            OutputUserBlockStatusList list = new OutputUserBlockStatusList();
            list.setStatusList(messagesStore.getUserStatusList());
            RestResult result = RestResult.ok(list);
            response.setContent(new Gson().toJson(result));
        }
        return true;
    }
}

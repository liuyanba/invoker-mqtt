package com.secret.loServer.action;

import com.secret.loServer.handler.Request;
import com.secret.loServer.handler.Response;

/**
 * 默认的主页Action，当访问主页且没有定义主页Action时，调用此Action
 * @author Looly
 *
 */
public class DefaultIndexAction extends Action{
    @Override
    public boolean action(Request request, Response response) {
        response.setContent("Welcome to LoServer.");
        return true;
    }
}

package com.secret.loServer.handler;


import com.secret.loServer.ServerSetting;
import com.secret.loServer.action.Action;
import com.secret.loServer.action.FileAction;
import com.secret.loServer.action.UnknownErrorAction;
import com.secret.loServer.filter.Filter;
import com.xiaoleilu.hutool.lang.Singleton;

import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.Utility;

/**
 * Action处理单元
 *
 * @author Looly
 */
abstract public class ActionHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger Logger = LoggerFactory.getLogger(ActionHandler.class);

    public ActionHandler(IMessagesStore messagesStore, ISessionsStore sessionsStore) {
        Action.messagesStore = messagesStore;
        Action.sessionsStore = sessionsStore;
    }

    @Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        Logger.info("Http request whit url {}", fullHttpRequest.uri());
		final Request request = Request.build(ctx, fullHttpRequest);
		final Response response = Response.build(ctx, request);

		response.setContentType("application/json");
		try {
			//do filter
			boolean isPass = this.doFilter(request, response);

			if(isPass){
				//do action
				this.doAction(ctx, request, response);
			}
		} catch (Exception e) {
		    e.printStackTrace();
            Utility.printExecption(Logger, e);
			Action errorAction = ServerSetting.getErrorAction(ServerSetting.MAPPING_ERROR);
			request.putParam(UnknownErrorAction.ERROR_PARAM_NAME, e);
			response.setContent(e.toString());
			errorAction.doAction(request, response);

            //如果发送请求未被触发，则触发之，否则跳过。
            if(!response.isSent()){
                response.send();
            }

            ctx.fireExceptionCaught(e);
            ctx.close();
		}


	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Logger.warn("{}", cause.getMessage());
        Channel channel = ctx.channel();
        if(channel.isActive())ctx.close();
	}

	//---------------------------------------------------------------------------------------- Private method start
	/**
	 * 执行过滤
	 * @param request 请求
	 * @param response 响应
	 * @@return  是否继续
	 */
	private boolean doFilter(Request request, Response response) {
		//全局过滤器
		Filter filter = ServerSetting.getFilter(ServerSetting.MAPPING_ALL);
		if(null != filter){
			if(false == filter.doFilter(request, response)){
				return false;
			}
		}

		//自定义Path过滤器
		filter = ServerSetting.getFilter(request.getPath());
		if(null != filter){
			if(false == filter.doFilter(request, response)){
				return false;
			}
		}

		return true;
	}

	abstract boolean isValidePath(String path);
	/**
	 * 执行Action
	 * @param request 请求对象
	 * @param response 响应对象
	 */
	private void doAction(ChannelHandlerContext ctx, Request request, Response response){
	    if(("/route".equalsIgnoreCase(request.getPath()) || request.getPath().startsWith("/fs/"))
            && "OPTIONS".equalsIgnoreCase(request.getMethod())){
	        handleOptions(ctx, request, response);
	        return;
        }
		Action action;
		if (isValidePath(request.getPath())) {
            action = ServerSetting.getAction(request.getPath(), request.getMethod().toUpperCase());
        } else {
            action = ServerSetting.getErrorAction(ServerSetting.MAPPING_ERROR);
        }

		if (action == null && (request.getPath().startsWith("/api") || request.getPath().startsWith("/admin"))) {
		    action = ServerSetting.getErrorAction(ServerSetting.MAPPING_ERROR);
        }

		if (null == action) {
			//查找匹配所有路径的Action
			action = ServerSetting.getAction(ServerSetting.MAPPING_ALL, request.getMethod());
			if(null == action){
				// 非Action方法，调用静态文件读取
				action = Singleton.get(FileAction.class);
			}
		}
        action.ctx = ctx;

		boolean isSync = action.doAction(request, response);

		if(isSync) {
            //如果发送请求未被触发，则触发之，否则跳过。
            if(!response.isSent()){
                response.send();
            }
        }
	}
	//---------------------------------------------------------------------------------------- Private method start


    private void handleOptions(ChannelHandlerContext ctx, Request request, Response response){
	    response.setHeader("Access-Control-Allow-Origin", "*");
	    response.setHeader("Access-Control-Allow-Headers", "*");
	    response.send();
    }
}

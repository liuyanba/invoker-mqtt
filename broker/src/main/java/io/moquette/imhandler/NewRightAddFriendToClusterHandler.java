package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.WFCMessage;
import com.alibaba.fastjson.JSONObject;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.IMTopic;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Liu_Yan-admin
 * @create 2021/3/16 14:51
 * @description 新版添加好友, 调用后直接添加成功, 但属于单向添加
 * @Version: 1.0
 */
@Handler(IMTopic.NewAddFriendToClusterTopic)
public class NewRightAddFriendToClusterHandler extends IMHandler<WFCMessage.PullUserRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(NewRightAddFriendToClusterHandler.class);


    /**
     * @param ackPayload 负责输出流
     * @param clientID   客户端Id
     * @param fromUser   用户id
     * @param isAdmin    是否管理员
     * @param request    请求体
     * @param callback   回调
     * @return ErrorCode
     */
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullUserRequest request, Qos1PublishHandler.IMCallback callback) {
        LOG.info("AFNL 集群  进入Action方法!");
        Map resultMap = new HashMap();
        resultMap.put("code", ErrorCode.ERROR_CODE_SUCCESS);
        String resultjson = JSONObject.toJSON(resultMap).toString();
        LOG.info("AFNL LIU YAN Result:{}", resultjson);
        ackPayload.writeBytes(resultjson.getBytes());
        //只要告诉前端 是否成功
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

}
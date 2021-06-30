package io.moquette.interception;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.BrokerConstants.CONNECTION_TOPIC;

/**
 * @author Liu_Yan-admin
 * @create 2020/12/24 15:49
 * @description 老刘handler
 * @Version: 1.0
 */
public class ConnectionInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionInterceptHandler.class);
    private final HazelcastInstance hz;

    @Override
    public String getID() {
        return ConnectionInterceptHandler.class.getName() + "@" + hz.getName();
    }

    public ConnectionInterceptHandler(Server server) {
        this.hz = server.getHazelcastInstance();
    }

    /**
     * msg.payload.clientIdentifier: 客户端id
     * msg.payload.userName: JpLrJrWW 用户名
     *
     * 思路: 将该用户推送出去,那边监听到后更新用户信息即可
     * @param msg
     */
    @Override
    public void onConnect(InterceptConnectMessage msg) {
        ConnectionMsg connectionMsg = new ConnectionMsg(msg);
        ITopic<ConnectionMsg> topic = hz.getTopic(CONNECTION_TOPIC);
        topic.publish(connectionMsg);
        LOG.info("更新其他节点的该客户端连接信息: clientId= {} , userName= {}", msg.getClientID(), msg.getUsername());
    }

}
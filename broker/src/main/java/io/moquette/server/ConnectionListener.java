package io.moquette.server;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.xiaoleilu.hutool.util.DateUtil;
import io.moquette.interception.ConnectionMsg;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.ISessionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

/**
 * @author Liu_Yan-admin
 * @Create: 2020/12/24 18:11
 * @Description: 老刘连接监听器
 * @Version: 1.0
 */
public class ConnectionListener implements MessageListener<ConnectionMsg> {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastListener.class);

    private final Server server;

    public ConnectionListener(Server server) {
        this.server = server;
    }


    @Override
    public void onMessage(Message<ConnectionMsg> msg) {
        if (!msg.getPublishingMember().equals(server.getHazelcastInstance().getCluster().getLocalMember())) {
            ConnectionMsg connectionMsg = msg.getMessageObject();
            String clientId = connectionMsg.getClientId();
            String userName = connectionMsg.getUserName();
            ISessionsStore sessionsStore = server.getStore().sessionsStore();
            sessionsStore.loadUserSession(userName, clientId);
            MemorySessionStore.Session session = sessionsStore.getSession(clientId);
            if (Objects.isNull(session)) {
                LOG.info("接受到集群广播更新连接消息: ClientId={} , userName={} , 但并未找到对应session", clientId, userName);
                return;
            }

            session.refreshLastActiveTime();
            server.getStore().sessionsStore().updateUserSessionActive(session);

            String lastActiveTime = DateUtil.format(new Date(session.getLastActiveTime()), DateUtil.NORM_DATETIME_MS_PATTERN);
            LOG.info("接受到集群广播连接消息: ClientId={} , userName={} , 已更新该session最后生存时间:{}", clientId, userName, lastActiveTime);
        }
    }


}
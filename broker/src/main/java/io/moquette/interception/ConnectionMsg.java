package io.moquette.interception;

import io.moquette.interception.messages.InterceptConnectMessage;

import java.io.Serializable;

/**
 * @author Liu_Yan-admin
 * @create 2020/12/24 17:50
 * @description 老刘连接对象
 * @Version: 1.0
 */
public class ConnectionMsg implements Serializable {

    private static final long serialVersionUID = -4970539243233693177L;

    private final String topicName;

    private final String clientId;

    private final String userName;

    public ConnectionMsg(InterceptConnectMessage msg) {
        this.clientId = msg.getClientID();
        this.userName = msg.getUsername();
        this.topicName = msg.getWillTopic();
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserName() {
        return userName;
    }

}
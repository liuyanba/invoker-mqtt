package io.moquette.liuyan.contants;

public interface LaoLiuConstants {

    interface HazelcastConstants {

        /**
         * 集群连接信息
         */
        String CLUSTER_CONNECTION_MAP = "cluster_connection_map";

        /**
         * 存储目标对应的clientId
         */
        String TARGET_USERNAME_MAP = "message_target";

        /**
         * 不存在的节点编号
         */
        int NON_EXIST_NODE = -1;

        /**
         * 标识1
         */
        int ONE = 1;

        /**
         * 标识0
         */
        int ZERO = 0;
    }

}

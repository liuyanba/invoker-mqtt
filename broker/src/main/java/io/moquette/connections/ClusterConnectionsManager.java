package io.moquette.connections;



/**
 * @Author: Liu_Yan
 * @date 2020年12月16日11点41分
 * 此接口将由外部代码库使用，以检索和关闭集群之间会话session的物理连接。
 */
public interface ClusterConnectionsManager {

    /**
     * 整个集群中是否有该Session存货
     * @return True or False
     */
    boolean isConnectedCluster(String clientId);

    /**
     * 返回集群中存货的所有连接数
     * @return 连接数
     */
    int getClusterActiveConnectionsNo();

    /**
     * 通过clientId获取所在集群节点id
     * @param clientId 客户端id
     * @return 所在节点机器NodeId
     */
    int getClusterNodeByClientId(String clientId);
}

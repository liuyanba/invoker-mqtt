package io.moquette.liuyan.utils;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import io.moquette.BrokerConstants;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static io.moquette.liuyan.contants.LaoLiuConstants.HazelcastConstants.CLUSTER_CONNECTION_MAP;

/**
 * @author Liu_Yan-admin
 * @create 2021/3/9 10:33
 * @description
 * @Version: 1.0
 */
public class ZookeeperClientRegister {

    private static final int DEFAULT_TIMEOUT = 10000;
    private static final String DEFAULT_SERVER_PARENT = "/backend";
    private static ZooKeeper zkConnect = null;
    private static List<String> availableServers;
    private static HazelcastInstance hazelcastInstance;
    private static final Logger log = LoggerFactory.getLogger(ZookeeperClientRegister.class);

    /**
     * 连接至ZooKeeper
     *
     * @throws Exception
     */
    public static void connect(String hostAndAddress, HazelcastInstance instance) {
        hazelcastInstance = instance;
        try {
            zkConnect = new ZooKeeper(hostAndAddress, DEFAULT_TIMEOUT, watchedEvent -> {
                try {
                    updateServerCondition();    // 重复注册
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            LaoLiuUtils.errorLog("zk客户端连接时发生异常:{}", e);
        }
        System.out.println("已经连接zkClient成功!");
        log.info("已经连接zkClient成功!");
    }

    /**
     * 向zk查询服务器情况, 并update本地服务器列表
     *
     * @throws Exception
     */
    public static void updateServerCondition() {
        List<String> children = null;
        try {
            children = zkConnect.getChildren(DEFAULT_SERVER_PARENT, true);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        List<String> servers = new ArrayList<>();
        if (children != null) {
            for (String child : children) {
                byte[] data = new byte[0];
                try {
                    data = zkConnect.getData(DEFAULT_SERVER_PARENT + "/" + child,
                            false,
                            null);
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
                servers.add(new String(data));
            }
        }
        availableServers = servers;
        //首先找个地方存启动的nodeId
        ArrayList<Integer> zookeeperNodeIdList = new ArrayList<>();
        for (String availableServer : availableServers) {
            String nodeIdStr = availableServer.substring(availableServer.lastIndexOf("/"));
            nodeIdStr = nodeIdStr.replaceAll("/", "");
            zookeeperNodeIdList.add(Integer.valueOf(nodeIdStr));
        }
        System.out.println("zookeeperNodeIdList:" + zookeeperNodeIdList);
        //然后在这里判断 如果zookeeperNode里面缺少hazelcastNode某个nodeId  那么就认为该node是下线了  开始干掉缓存处理数据  2,3     1,2,3
        ISet<Integer> hazelcastNodeIdList = hazelcastInstance.getSet(BrokerConstants.NODE_IDS);
        log.info("hazelcast中保存的节点:{}", new Gson().toJson(hazelcastNodeIdList));

        //查看缺少的值
        ArrayList<Integer> hazelcastNodeList = new ArrayList<>(hazelcastNodeIdList);
        hazelcastNodeList.removeAll(zookeeperNodeIdList);
        if (LaoLiuUtils.isNotEmpty(hazelcastNodeList)) {
            log.error("已宕机节点:{}", hazelcastNodeList);
        }

        for (Integer lostNodeId : hazelcastNodeList) {
            //集群的 clientId以及NodeId 清空
            IMap<String, String> clusterMap = hazelcastInstance.getMap(CLUSTER_CONNECTION_MAP);
            Collection<String> values = clusterMap.values();
            values.removeIf(s -> s.equals(String.valueOf(lostNodeId)));

            //还需要清楚hazelcast里的节点信息 保证下次可以重新启动不冲突
            hazelcastNodeIdList.remove(lostNodeId);
            log.info("hazelcast中去除宕机后剩余节点:{}", new Gson().toJson(hazelcastNodeIdList));
        }
        System.out.println("ZK客户端检测到还存活的:    " + Arrays.toString(servers.toArray(new String[0])));
        log.info("ZK客户端检测到还存活的:    " + Arrays.toString(servers.toArray(new String[0])));
    }


    /**
     * 通过sleep让客户端持续运行，"监听"
     */
    public static void sleep() {
        System.out.println("client is working");
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LaoLiuUtils.errorLog("zk客户端休眠时发生异常:{}", e);
        }
    }

}
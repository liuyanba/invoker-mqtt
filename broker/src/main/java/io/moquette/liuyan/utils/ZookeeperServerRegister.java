package io.moquette.liuyan.utils;

import com.hazelcast.core.HazelcastInstance;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Liu_Yan-admin
 * @create 2021/3/8 20:31
 * @description
 * @Version: 1.0
 */
public class ZookeeperServerRegister {

    private static int sessionTimeOut = 10000;
    private static ZooKeeper zkClient;
    public static final String PARENT_NODE_NAME = "/backend";
    private static volatile ArrayList<String> servers;

    private static final Logger log = LoggerFactory.getLogger(ZookeeperServerRegister.class);

    public static void getConnection(String hostAndAddress) {
        try {
            zkClient = new ZooKeeper(hostAndAddress, sessionTimeOut, watchedEvent -> {
                try {
                    if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged && PARENT_NODE_NAME.equals(watchedEvent.getPath())) {
                        getAvailableServers();
                    }
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            LaoLiuUtils.errorLog("zk注册器连接时发生异常:{}", e);
        }
        System.out.println("已经连接zk成功!");
        log.info("已经连接zk成功!");
    }

    /**
     * 向ZooKeeper注册本服务器节点
     *
     * @param data 服务器信息
     * @throws Exception
     */
    public static void register(String data) {
        String create = null;   // 注册成ephemeral节点以便自动在zk上注销
        try {
            create = zkClient.create(data,
                    data.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            LaoLiuUtils.errorLog("zk注册器注册时发生异常:{}", e);
        }
        log.info(create + " 已经在zk注册成功!");
        System.out.println(create + " 已经在zk注册成功!");
    }


    public static void getAvailableServers() throws KeeperException, InterruptedException {
        ArrayList<String> serverList = new ArrayList<>();
        List<String> children = zkClient.getChildren(PARENT_NODE_NAME, true);
        for (String child : children) {
            byte[] data = zkClient.getData(PARENT_NODE_NAME + "/" + child, null, null);
            serverList.add(new String(data));
        }
        servers = serverList;
        log.info("存活的服务为 " + servers);
        System.out.println("存活的服务为 " + servers);
    }


    public static void zkServerRegister(String hostAndAddress, String data, HazelcastInstance instance) {
        ZookeeperServerRegister.getConnection(hostAndAddress);
        try {
            if (Objects.isNull(zkClient.exists(PARENT_NODE_NAME, false))) {
                ZookeeperServerRegister.register(PARENT_NODE_NAME);
            }

            if (Objects.isNull(zkClient.exists(PARENT_NODE_NAME + "/" + data, false))) {
                ZookeeperServerRegister.register(PARENT_NODE_NAME + "/" + data);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        ZookeeperClientRegister.connect(hostAndAddress,instance);
        ZookeeperClientRegister.updateServerCondition();
    }
}
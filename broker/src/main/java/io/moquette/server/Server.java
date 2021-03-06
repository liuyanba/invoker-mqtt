/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.server;

import cn.wildfirechat.push.PushServer;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.*;
import com.secret.loServer.LoServer;
import com.secret.loServer.ServerSetting;
import com.secret.loServer.action.admin.AdminAction;
import io.moquette.BrokerConstants;
import io.moquette.connections.IConnectionsManager;
import io.moquette.interception.ConnectionMsg;
import io.moquette.interception.HazelcastMsg;
import io.moquette.interception.InterceptHandler;
import io.moquette.liuyan.utils.ZookeeperServerRegister;
import io.moquette.persistence.MemoryStorageService;
import io.moquette.persistence.RPCCenter;
import io.moquette.server.config.*;
import io.moquette.server.netty.NettyAcceptor;
import io.moquette.spi.IStore;
import io.moquette.spi.impl.ProtocolProcessor;
import io.moquette.spi.impl.ProtocolProcessorBootstrapper;
import io.moquette.spi.impl.security.AES;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.security.ISslContextCreator;
import io.moquette.spi.security.Tokenor;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.DBUtil;
import com.liuyan.im.MessageShardingUtil;
import com.liuyan.im.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

import static io.moquette.BrokerConstants.*;
import static io.moquette.logging.LoggingUtils.getInterceptorIds;

/**
 * Launch a configured version of the server.
 */
public class Server {
    private final static String BANNER =
            " _____    ___       __      __     ______   __  __               _____      \n" +
                    "/\\  __`\\ /\\_ \\     /\\ \\    /\\ \\   /\\__  _\\ /\\ \\/\\ \\      /'\\_/`\\/\\  __`\\    \n" +
                    "\\ \\ \\/\\ \\\\//\\ \\    \\_\\ \\   \\ \\ \\  \\/_/\\ \\/ \\ \\ \\ \\ \\    /\\      \\ \\ \\/\\ \\   \n" +
                    " \\ \\ \\ \\ \\ \\ \\ \\   /'_` \\   \\ \\ \\  __\\ \\ \\  \\ \\ \\ \\ \\   \\ \\ \\__\\ \\ \\ \\ \\ \\  \n" +
                    "  \\ \\ \\_\\ \\ \\_\\ \\_/\\ \\L\\ \\   \\ \\ \\L\\ \\\\_\\ \\__\\ \\ \\_\\ \\   \\ \\ \\_/\\ \\ \\ \\\\'\\\\ \n" +
                    "   \\ \\_____\\/\\____\\ \\___,_\\   \\ \\____//\\_____\\\\ \\_____\\   \\ \\_\\\\ \\_\\ \\___\\_\\\n" +
                    "    \\/_____/\\/____/\\/__,_ /    \\/___/ \\/_____/ \\/_____/    \\/_/ \\/_/\\/__//_/\n" +
                    "                                                                            \n";


    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static Server instance;

    public static Server getServer() {
        return instance;
    }

    private ServerAcceptor m_acceptor;

    private boolean m_shutdowning = false;

    public volatile boolean m_initialized;

    private ProtocolProcessor m_processor;

    private HazelcastInstance hazelcastInstance;

    private ProtocolProcessorBootstrapper m_processorBootstrapper;

    private ThreadPoolExecutorWrapper dbScheduler;
    private ThreadPoolExecutorWrapper imBusinessScheduler;

    private IConfig mConfig;

    private IStore m_store;

    static {
        System.out.println(BANNER);
    }

    public static void start(String[] args) throws IOException {
        instance = new Server();
        final IConfig config = defaultConfig();

        System.setProperty("hazelcast.logging.type", "none");
        instance.mConfig = config;
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        //TODO: ??????
        instance.startServer(config);

        int httpLocalPort = Integer.parseInt(config.getProperty(BrokerConstants.HTTP_LOCAL_PORT));
        int httpAdminPort = Integer.parseInt(config.getProperty(BrokerConstants.HTTP_ADMIN_PORT));

        AdminAction.setSecretKey(config.getProperty(HTTP_SERVER_SECRET_KEY));
        AdminAction.setNoCheckTime(config.getProperty(HTTP_SERVER_API_NO_CHECK_TIME));

        final LoServer httpServer = new LoServer(httpLocalPort, httpAdminPort, instance.m_processor.getMessagesStore(), instance.m_store.sessionsStore());
        try {
            httpServer.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

        final PushServer pushServer = PushServer.getServer();
        pushServer.init(config, instance.getStore().sessionsStore());

        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(instance::stopServer));
        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::shutdown));

        //??????ZKServer
        ZookeeperServerRegister.zkServerRegister(config.getProperty("zk_address"), config.getProperty(BrokerConstants.NODE_ID), instance.getHazelcastInstance());

        System.out.println("Wildfire IM?????????????????????...");
    }

    /**
     * Starts Moquette bringing the configuration from the file located at m_config/wildfirechat.conf
     *
     * @throws IOException in case of any IO error.
     */
    public void startServer() throws IOException {
        final IConfig config = defaultConfig();
        startServer(config);
    }

    public static IConfig defaultConfig() {
        File defaultConfigurationFile = defaultConfigFile();
        LOG.info("??????Moquette??????????????????????????????= {}", defaultConfigurationFile.getAbsolutePath());
        IResourceLoader filesystemLoader = new FileResourceLoader(defaultConfigurationFile);
        return new ResourceLoaderConfig(filesystemLoader);
    }

    private static File defaultConfigFile() {
        String configPath = System.getProperty("wildfirechat.path", null);
        return new File(configPath, IConfig.DEFAULT_CONFIG);
    }


    /**
     * Starts Moquette bringing the configuration from the given file
     *
     * @param configFile text file that contains the configuration.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(File configFile) throws IOException {
        LOG.info("??????Moquette??????????????????????????????= {}", configFile.getAbsolutePath());
        IResourceLoader filesystemLoader = new FileResourceLoader(configFile);
        final IConfig config = new ResourceLoaderConfig(filesystemLoader);
        startServer(config);
    }

    /**
     * Starts the server with the given properties.
     *
     * @param configProps the properties map to use as configuration.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(Properties configProps) throws IOException {
        LOG.info("????????????????????????Moquette?????????");
        final IConfig config = new MemoryConfig(configProps);
        startServer(config);
    }

    /**
     * Starts Moquette bringing the configuration files from the given Config implementation.
     *
     * @param config the configuration to use to start the broker.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(IConfig config) throws IOException {
        LOG.info("??????IConfig????????????Moquette?????????...");
        startServer(config, null);
    }

    /**
     * Starts Moquette with config provided by an implementation of IConfig class and with the set
     * of InterceptHandler.
     *
     * @param config   the configuration to use to start the broker.
     * @param handlers the handlers to install in the broker.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(IConfig config, List<? extends InterceptHandler> handlers) throws IOException {
        LOG.info("??????IConfig?????????????????????????????????Moquette?????????");
        startServer(config, handlers, null, null, null);
    }

    public void startServer(IConfig config, List<? extends InterceptHandler> handlers, ISslContextCreator sslCtxCreator,
                            IAuthenticator authenticator, IAuthorizator authorizator) throws IOException {
        if (handlers == null) {
            handlers = Collections.emptyList();
        }
        DBUtil.init(config);
        String strKey = config.getProperty(BrokerConstants.CLIENT_PROTO_SECRET_KEY);
        String[] strs = strKey.split(",");
        if (strs.length != 16) {
            LOG.error("???????????????????????????16");
        }
        byte[] keys = new byte[16];
        for (int i = 0; i < 16; i++) {
            keys[i] = (byte) (Integer.parseInt(strs[i].replace("0X", "").replace("0x", ""), 16));
        }


        AES.init(keys);

        LOG.info("??????Moquette???????????? MQTT???????????????= {}", getInterceptorIds(handlers));

        int threadNum = Runtime.getRuntime().availableProcessors() * 2;
        dbScheduler = new ThreadPoolExecutorWrapper(Executors.newScheduledThreadPool(threadNum), threadNum, "db");
        imBusinessScheduler = new ThreadPoolExecutorWrapper(Executors.newScheduledThreadPool(threadNum), threadNum, "business");

        //TODO: ???????????????????????????INTERCEPT_HANDLER_PROPERTY_NAME = "intercept.handler"?????????config???????????????????????????????????????????????????????????????
        final String handlerProp = System.getProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME);
        if (handlerProp != null) {
            config.setProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME, handlerProp);
        }

        //TODO: 2020???12???24???15???37?????????  ???????????????handler
        final String connHandlerProp = System.getProperty(BrokerConstants.INTERCEPT_CONNECTION_HANDLER_PROPERTY_NAME);
        if (handlerProp != null) {
            config.setProperty(BrokerConstants.INTERCEPT_CONNECTION_HANDLER_PROPERTY_NAME, connHandlerProp);
        }

        initMediaServerConfig(config);

        final String persistencePath = config.getProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME);
        LOG.info("??????????????????????????????????????????= {}", persistencePath);
        m_store = initStore(config, this);
        m_processorBootstrapper = new ProtocolProcessorBootstrapper(); //TODO:?????????ProtocolProcessor

        //TODO:configureCluster(config);????????????
        boolean configured = configureCluster(config);

        m_store.initStore();
        final ProtocolProcessor processor = m_processorBootstrapper.init(config, handlers, authenticator, authorizator,
                this, m_store);
        LOG.info("????????????MQTT???????????????");
        if (sslCtxCreator == null) {
            LOG.warn("???????????????SSL??????????????????");
            sslCtxCreator = new DefaultMoquetteSslContextCreator(config);
        }

        m_processor = processor;

        LOG.info("????????????????????????????????????");
        m_acceptor = new NettyAcceptor();
        m_acceptor.initialize(processor, config, sslCtxCreator);


        LOG.info("Moquette???????????????????????????!");
        m_initialized = configured;
    }

    private IStore initStore(IConfig props, Server server) {
        LOG.info("????????? messages ??? sessions stores...");
        IStore store = instantiateConfiguredStore(props, server.getDbScheduler(), server);
        if (store == null) {
            throw new IllegalArgumentException("?????????????????????");
        }
        return store;
    }

    private IStore instantiateConfiguredStore(IConfig props,
                                              ThreadPoolExecutorWrapper scheduledExecutor, Server server) {
        return new MemoryStorageService(props, scheduledExecutor, server);
    }

    public IStore getStore() {
        return m_store;
    }

    private void initMediaServerConfig(IConfig config) {
        MediaServerConfig.QINIU_ACCESS_KEY = config.getProperty(BrokerConstants.QINIU_ACCESS_KEY, MediaServerConfig.QINIU_ACCESS_KEY);
        MediaServerConfig.QINIU_SECRET_KEY = config.getProperty(BrokerConstants.QINIU_SECRET_KEY, MediaServerConfig.QINIU_SECRET_KEY);
        MediaServerConfig.QINIU_SERVER_URL = config.getProperty(BrokerConstants.QINIU_SERVER_URL, MediaServerConfig.QINIU_SERVER_URL);
        if (MediaServerConfig.QINIU_SERVER_URL.contains("//")) {
            MediaServerConfig.QINIU_SERVER_URL = MediaServerConfig.QINIU_SERVER_URL.substring(MediaServerConfig.QINIU_SERVER_URL.indexOf("//") + 2);
        }

        MediaServerConfig.QINIU_BUCKET_GENERAL_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_GENERAL_NAME);
        MediaServerConfig.QINIU_BUCKET_GENERAL_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_GENERAL_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_IMAGE_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_IMAGE_NAME);
        MediaServerConfig.QINIU_BUCKET_IMAGE_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_IMAGE_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_VOICE_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_VOICE_NAME);
        MediaServerConfig.QINIU_BUCKET_VOICE_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_VOICE_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_VIDEO_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_VIDEO_NAME);
        MediaServerConfig.QINIU_BUCKET_VIDEO_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_VIDEO_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_FILE_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_FILE_NAME);
        MediaServerConfig.QINIU_BUCKET_FILE_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_FILE_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_STICKER_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_STICKER_NAME);
        MediaServerConfig.QINIU_BUCKET_STICKER_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_STICKER_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_MOMENTS_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_MOMENTS_NAME);
        MediaServerConfig.QINIU_BUCKET_MOMENTS_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_MOMENTS_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_PORTRAIT_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_PORTRAIT_NAME);
        MediaServerConfig.QINIU_BUCKET_PORTRAIT_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_PORTRAIT_DOMAIN);

        MediaServerConfig.QINIU_BUCKET_FAVORITE_NAME = config.getProperty(BrokerConstants.QINIU_BUCKET_FAVORITE_NAME);
        MediaServerConfig.QINIU_BUCKET_FAVORITE_DOMAIN = config.getProperty(BrokerConstants.QINIU_BUCKET_FAVORITE_DOMAIN);


        MediaServerConfig.SERVER_IP = getServerIp(config);

        MediaServerConfig.HTTP_SERVER_PORT = Integer.parseInt(config.getProperty(BrokerConstants.HTTP_SERVER_PORT));

        MediaServerConfig.FILE_STROAGE_ROOT = config.getProperty(BrokerConstants.FILE_STORAGE_ROOT, MediaServerConfig.FILE_STROAGE_ROOT);
        File file = new File(MediaServerConfig.FILE_STROAGE_ROOT);
        if (!file.exists()) {
            file.mkdirs();
        }
        ServerSetting.setRoot(file);

        MediaServerConfig.USER_QINIU = Integer.parseInt(config.getProperty(BrokerConstants.USER_QINIU)) > 0;
    }

    private String getServerIp(IConfig config) {
        String serverIp = config.getProperty(BrokerConstants.SERVER_IP_PROPERTY_NAME);

        if (serverIp == null || serverIp.equals("0.0.0.0")) {
            serverIp = Utility.getLocalAddress().getHostAddress();
        }
        return serverIp;
    }

    private boolean configureCluster(IConfig config) throws FileNotFoundException {
        LOG.info("???????????????Hazelcast??????");
        String serverIp = getServerIp(config);

        String hzConfigPath = config.getProperty(BrokerConstants.HAZELCAST_CONFIGURATION);
        String hzClientIp = config.getProperty(BrokerConstants.HAZELCAST_CLIENT_IP, "localhost");
        String hzClientPort = config.getProperty(BrokerConstants.HAZELCAST_CLIENT_PORT, "5703");
        //TODO: ?????????????????????HazelcastInterceptHandler?????????????????????????????????
        //TODO: ???config????????????hazelcast.configuration???????????????????????????????????????????????????HazelcastInstance??????????????????????????????distribution/src/main/resources?????????????????????????????????????????????
        if (hzConfigPath != null) {
            boolean isHzConfigOnClasspath = this.getClass().getClassLoader().getResource(hzConfigPath) != null;
            Config hzconfig = isHzConfigOnClasspath
                    ? new ClasspathXmlConfig(hzConfigPath)
                    : new FileSystemXmlConfig(hzConfigPath);
            LOG.info("??????Hazelcast????????? ConfigurationFile = {}", hzconfig);
            hazelcastInstance = Hazelcast.newHazelcastInstance(hzconfig);
        } else {
            LOG.info("????????????????????????Hazelcast??????");
            hazelcastInstance = Hazelcast.newHazelcastInstance();
        }


        String longPort = config.getProperty(BrokerConstants.PORT_PROPERTY_NAME);
        String shortPort = config.getProperty(BrokerConstants.HTTP_SERVER_PORT);
        String nodeIdStr = config.getProperty(BrokerConstants.NODE_ID);
        ISet<Integer> nodeIdSet = hazelcastInstance.getSet(BrokerConstants.NODE_IDS);
        int nodeId;
        try {
            nodeId = Integer.parseInt(nodeIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("nodeId error: " + nodeIdStr);
        }
        if (nodeIdSet != null && nodeIdSet.contains(nodeId)) {
            LOG.error("????????????????????????????????????????????????????????????????????????");
            System.exit(-1);
        }

        MessageShardingUtil.setNodeId(nodeId);
        nodeIdSet.add(nodeId);

        hazelcastInstance.getCluster().getLocalMember().setStringAttribute(HZ_Cluster_Node_External_Long_Port, longPort);
        hazelcastInstance.getCluster().getLocalMember().setStringAttribute(HZ_Cluster_Node_External_Short_Port, shortPort);
        hazelcastInstance.getCluster().getLocalMember().setIntAttribute(HZ_Cluster_Node_ID, nodeId);
        hazelcastInstance.getCluster().getLocalMember().setStringAttribute(HZ_Cluster_Node_External_IP, serverIp);
        Tokenor.setKey(config.getProperty(BrokerConstants.TOKEN_SECRET_KEY));
        String expirTimeStr = config.getProperty(TOKEN_EXPIRE_TIME);
        if (!StringUtil.isNullOrEmpty(expirTimeStr)) {
            try {
                Tokenor.setExpiredTime(Long.parseLong(expirTimeStr));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        RPCCenter.getInstance().init(this);

        listenOnHazelCastMsg();
        return true;
    }

    private void listenOnHazelCastMsg() {
        LOG.info("??????Hazelcast????????? TopicName={}", "moquette");
        HazelcastInstance hz = getHazelcastInstance();
        ITopic<HazelcastMsg> topic = hz.getTopic("moquette");
        //TODO: ??????HazelcastInstance???????????????????????????HazelcastListener?????????????????????HazelcastInterceptHandler???????????????????????????????????????????????????
        topic.addMessageListener(new HazelcastListener(this));

        //TODO: ??????HazelcastInstance,????????????????????????ConnectionListener ????????????ConnectionInterceptHandler????????????,????????????????????????????????????
        LOG.info("??????Connection?????? TopicName={}", CONNECTION_TOPIC);
        ITopic<ConnectionMsg> connectionTopic = hz.getTopic(CONNECTION_TOPIC);
        connectionTopic.addMessageListener(new ConnectionListener(this));
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    public void internalRpcMsg(String fromUser, String clientId, byte[] message, int messageId, String from, String request, boolean isAdmin) {

        if (!m_initialized) {
            LOG.error("Moquette????????????????????????????????????");
            return;
        }
        LOG.debug("internalNotifyMsg");
        m_processor.onRpcMsg(fromUser, clientId, message, messageId, from, request, isAdmin);
    }

    public boolean isShutdowning() {
        return m_shutdowning;
    }

    public void stopServer() {
        System.out.println("?????????????????????????????????????????????db????????????5?????????");

        LOG.info("???????????????????????????????????????");
        m_shutdowning = true;

        m_acceptor.close();
        LOG.trace("??????MQTT???????????????");
        m_processorBootstrapper.shutdown();
        m_initialized = false;
        if (hazelcastInstance != null) {
            LOG.trace("???????????????Hazelcast??????");
            try {
                hazelcastInstance.shutdown();
            } catch (HazelcastInstanceNotActiveException e) {
                LOG.warn("?????????Hazelcast??????????????????");
            }
        }

        dbScheduler.shutdown();
        imBusinessScheduler.shutdown();

        LOG.info("Moquette?????????????????????");
    }

    /**
     * SPI method used by Broker embedded applications to add intercept handlers.
     *
     * @param interceptHandler the handler to add.
     */
    public void addInterceptHandler(InterceptHandler interceptHandler) {
        if (!m_initialized) {
            LOG.error("Moquette????????????????????????MQTT?????????????????? InterceptorId = {}",
                    interceptHandler.getID());
            throw new IllegalStateException("???????????????????????????????????????????????????");
        }
        LOG.info("??????MQTT?????????????????? InterceptorId = {}", interceptHandler.getID());
        m_processor.addInterceptHandler(interceptHandler);
    }

    /**
     * SPI method used by Broker embedded applications to remove intercept handlers.
     *
     * @param interceptHandler the handler to remove.
     */
    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        if (!m_initialized) {
            LOG.error("Moquette????????????????????????MQTT?????????????????? InterceptorId = {}",
                    interceptHandler.getID());
            throw new IllegalStateException("??????????????????????????????????????????????????????");
        }
        LOG.info("??????MQTT?????????????????? InterceptorId = {}", interceptHandler.getID());
        m_processor.removeInterceptHandler(interceptHandler);
    }

    public IConfig getConfig() {
        return mConfig;
    }

    /**
     * Returns the connections manager of this broker.
     *
     * @return IConnectionsManager the instance used bt the broker.
     */
    public IConnectionsManager getConnectionsManager() {
        return m_processorBootstrapper.getConnectionDescriptors();
    }

    public ProtocolProcessor getProcessor() {
        return m_processor;
    }

    public ThreadPoolExecutorWrapper getDbScheduler() {
        return dbScheduler;
    }

    public ThreadPoolExecutorWrapper getImBusinessScheduler() {
        return imBusinessScheduler;
    }
}

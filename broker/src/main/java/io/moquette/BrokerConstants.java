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

package io.moquette;

import java.io.File;

public final class BrokerConstants {

    public static final String INTERCEPT_HANDLER_PROPERTY_NAME = "intercept.handler";
    public static final String BROKER_INTERCEPTOR_THREAD_POOL_SIZE = "intercept.thread_pool.size";
    public static final String PERSISTENT_STORE_PROPERTY_NAME = "persistent_store";
    public static final String SERVER_IP_PROPERTY_NAME = "server.ip";
    public static final String PORT_PROPERTY_NAME = "port";
    public static final String HOST_PROPERTY_NAME = "host";
    public static final String HTTP_SERVER_PORT = "http_port";
    public static final String HTTP_LOCAL_PORT = "local_port";

    public static final String HTTP_ADMIN_PORT = "http.admin.port";
    public static final String NODE_ID = "node_id";
    public static final String NODE_IDS = "node_ids";
    public static final String DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME = "moquette_store.mapdb";
    public static final String DEFAULT_PERSISTENT_PATH = System.getProperty("user.dir") + File.separator
            + DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
    public static final String WEB_SOCKET_PORT_PROPERTY_NAME = "websocket_port";
    public static final String WSS_PORT_PROPERTY_NAME = "secure_websocket_port";
    public static final String SSL_PORT_PROPERTY_NAME = "ssl_port";
    public static final String JKS_PATH_PROPERTY_NAME = "jks_path";
    public static final String KEY_STORE_PASSWORD_PROPERTY_NAME = "key_store_password";
    public static final String KEY_MANAGER_PASSWORD_PROPERTY_NAME = "key_manager_password";
    public static final String AUTHORIZATOR_CLASS_NAME = "authorizator_class";
    public static final String DB_AUTHENTICATOR_DRIVER = "authenticator.db.driver";
    public static final String DB_AUTHENTICATOR_URL = "authenticator.db.url";
    public static final String DB_AUTHENTICATOR_QUERY = "authenticator.db.query";
    public static final String DB_AUTHENTICATOR_DIGEST = "authenticator.db.digest";
    public static final int PORT = 1883;
    public static final int WEBSOCKET_PORT = 8080;
    public static final String DISABLED_PORT_BIND = "disabled";
    public static final String HOST = "0.0.0.0";
    public static final String NEED_CLIENT_AUTH = "need_client_auth";
    public static final String HAZELCAST_CONFIGURATION = "hazelcast.configuration";
    public static final String HAZELCAST_CLIENT_IP = "hazelcast.client.ip";
    public static final String HAZELCAST_CLIENT_PORT = "hazelcast.client.port";
    public static final String NETTY_SO_BACKLOG_PROPERTY_NAME = "netty.so_backlog";
    public static final String NETTY_SO_REUSEADDR_PROPERTY_NAME = "netty.so_reuseaddr";
    public static final String NETTY_TCP_NODELAY_PROPERTY_NAME = "netty.tcp_nodelay";
    public static final String NETTY_SO_KEEPALIVE_PROPERTY_NAME = "netty.so_keepalive";
    public static final String NETTY_CHANNEL_TIMEOUT_SECONDS_PROPERTY_NAME = "netty.channel_timeout.seconds";
    public static final String NETTY_EPOLL_PROPERTY_NAME = "netty.epoll";

    public static final String STORAGE_CLASS_NAME = "storage_class";

    public static final String QINIU_SERVER_URL = "qiniu.server_url";
    public static final String QINIU_ACCESS_KEY = "qiniu.access_key";
    public static final String QINIU_SECRET_KEY = "qiniu.secret_key";

    public static final String QINIU_BUCKET_GENERAL_NAME = "qiniu.bucket_general_name";
    public static final String QINIU_BUCKET_GENERAL_DOMAIN = "qiniu.bucket_general_domain";
    public static final String QINIU_BUCKET_IMAGE_NAME = "qiniu.bucket_image_name";
    public static final String QINIU_BUCKET_IMAGE_DOMAIN = "qiniu.bucket_image_domain";
    public static final String QINIU_BUCKET_VOICE_NAME = "qiniu.bucket_voice_name";
    public static final String QINIU_BUCKET_VOICE_DOMAIN = "qiniu.bucket_voice_domain";
    public static final String QINIU_BUCKET_VIDEO_NAME = "qiniu.bucket_video_name";
    public static final String QINIU_BUCKET_VIDEO_DOMAIN = "qiniu.bucket_video_domain";
    public static final String QINIU_BUCKET_FILE_NAME = "qiniu.bucket_file_name";
    public static final String QINIU_BUCKET_FILE_DOMAIN = "qiniu.bucket_file_domain";
    public static final String QINIU_BUCKET_STICKER_NAME = "qiniu.bucket_sticker_name";
    public static final String QINIU_BUCKET_STICKER_DOMAIN = "qiniu.bucket_sticker_domain";
    public static final String QINIU_BUCKET_MOMENTS_NAME = "qiniu.bucket_moments_name";
    public static final String QINIU_BUCKET_MOMENTS_DOMAIN = "qiniu.bucket_moments_domain";
    public static final String QINIU_BUCKET_PORTRAIT_NAME = "qiniu.bucket_portrait_name";
    public static final String QINIU_BUCKET_PORTRAIT_DOMAIN = "qiniu.bucket_portrait_domain";
    public static final String QINIU_BUCKET_FAVORITE_NAME = "qiniu.bucket_favorite_name";
    public static final String QINIU_BUCKET_FAVORITE_DOMAIN = "qiniu.bucket_favorite_domain";

    public static final String FILE_STORAGE_ROOT = "local.media.storage.root";
    public static final String USER_QINIU = "media.server.use_qiniu";

    public static final String PUSH_ANDROID_SERVER_ADDRESS = "push.android.server.address";
    public static final String PUSH_IOS_SERVER_ADDRESS = "push.ios.server.address";

    public static final String USER_ONLINE_STATUS_CALLBACK = "user.online_status_callback";

    public static final String HZ_Cluster_Node_External_IP = "node_external_ip";
    public static final String HZ_Cluster_Node_External_Long_Port = "node_external_long_port";
    public static final String HZ_Cluster_Node_External_Short_Port = "node_external_short_port";
    public static final String HZ_Cluster_Node_ID = "node_id";
    public static final String HTTP_SERVER_SECRET_KEY = "http.admin.secret_key";
    public static final String HTTP_SERVER_API_NO_CHECK_TIME = "http.admin.no_check_time";

    public static final String CLIENT_PROTO_SECRET_KEY = "client.proto.secret_key";
    public static final String TOKEN_SECRET_KEY = "token.key";
    public static final String TOKEN_EXPIRE_TIME = "token.expire_time";

    public static final String EMBED_DB_PROPERTY_NAME = "embed.db";

    public static final String HZ_Cluster_Master_Node = "master_node";

    public static final String SENSITIVE_Filter_Type = "sensitive.filter.type";

    public static final String MESSAGE_Forward_Url = "message.forward.url";

    public static final String SERVER_MULTI_ENDPOINT = "server.multi_endpoint";
    public static final String SERVER_MULTI_PLATFROM_NOTIFICATION = "server.multi_platform_notification";

    public static final String MONGODB_Client_URI = "mongodb.client_uri";
    public static final String MONGODB_Database = "mongodb.database";
    public static final String MONGODB_Data_Expire_Days = "mongodb.data_expire_days";

    public static final String MESSAGE_ROAMING = "message.roaming";
    public static final String MESSAGE_Remote_History_Message = "message.remote_history_message";

    public static final String MESSAGE_Max_Queue = "message.max_queue";

    public static final String MESSAGE_Disable_Stranger_Chat = "message.disable_stranger_chat";

    public static final String MESSAGE_Blacklist_Strategy = "message.blacklist.strategy";

    public static final String FRIEND_Disable_Search = "friend.disable_search";
    public static final String FRIEND_Disable_NickName_Search = "friend.disable_nick_name_search";
    public static final String FRIEND_Disable_Friend_Request = "friend.disable_friend_request";
    public static final String FRIEND_Repeat_Request_Duration = "friend.repeat_request_duration";
    public static final String FRIEND_Reject_Request_Duration = "friend.reject_request_duration";
    public static final String FRIEND_Request_Expiration_Duration = "friend.request_expiration_duration";

    public static final String CHATROOM_Participant_Idle_Time = "chatroom.participant_idle_time";
    public static final String CHATROOM_Rejoin_When_Active = "chatroom.rejoin_when_active";

    public static final String MESSAGES_DISABLE_REMOTE_SEARCH = "message.disable_remote_search";

    //TODO: 2020年11月16日15点51分 增加
    public static final String WEB_SOCKET_PATH_PROPERTY_NAME = "websocket_path";
    public static final String WEBSOCKET_PATH = "/mqtt";
    public static final String WEB_SOCKET_MAX_FRAME_SIZE_PROPERTY_NAME = "websocket_max_frame_size";
    public static final String NETTY_MAX_BYTES_PROPERTY_NAME = "netty.mqtt.message_size";

    /**
     * 推送地址
     */
    public static final String MESSAGE_NOTIFY_Url = "message.notify.url";

    /**
     * 大数据文本审核地址
     */
    public static final String TEXT_REVIEW_URL = "message.review.url";

    /**
     */
    public static final String GET_PROFILE_URL = "cloud.profile.url";

    //TODO: 2020年12月24日15点38分 增加
    public static final String  INTERCEPT_CONNECTION_HANDLER_PROPERTY_NAME ="intercept.connection.handler";

    public static final String CONNECTION_TOPIC = "connectionLiu";
    public static final String NOTIFY_ADD_FRIEND = "message.friend.url";

    private BrokerConstants() {
    }
}

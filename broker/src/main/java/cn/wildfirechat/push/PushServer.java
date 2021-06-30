/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.push;

import cn.wildfirechat.proto.ProtoConstants;
import com.google.gson.Gson;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.server.config.IConfig;
import io.moquette.spi.ISessionsStore;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.HttpUtils;
import com.liuyan.im.Utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.moquette.BrokerConstants.*;

public class PushServer {
    private static final Logger LOG = LoggerFactory.getLogger(PushServer.class);

    public interface PushMessageType {
        int PUSH_MESSAGE_TYPE_NORMAL = 0;
        int PUSH_MESSAGE_TYPE_VOIP_INVITE = 1;
        int PUSH_MESSAGE_TYPE_VOIP_BYE = 2;
        int PUSH_MESSAGE_TYPE_FRIEND_REQUEST = 3;
    }

    private static PushServer INSTANCE = new PushServer();
    private ISessionsStore sessionsStore;
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5);

    private String androidPushServerUrl;
    private String iOSPushServerUrl;
    private String notifyUrl;

    private PushServer() {
    }

    public static PushServer getServer() {
        return INSTANCE;
    }

    public void init(IConfig config, ISessionsStore sessionsStore) {
        this.sessionsStore = sessionsStore;
        this.androidPushServerUrl = config.getProperty(PUSH_ANDROID_SERVER_ADDRESS);
        this.iOSPushServerUrl = config.getProperty(PUSH_IOS_SERVER_ADDRESS);
        this.iOSPushServerUrl = config.getProperty(PUSH_IOS_SERVER_ADDRESS);
        this.notifyUrl = config.getProperty(MESSAGE_NOTIFY_Url);
    }

    public void pushMessage(PushMessage pushMessage, String deviceId, String pushContent) {
        LOG.info("try to delivery push diviceId = {}, conversationType = {}, pushContent = {}", deviceId, pushMessage.convType, pushContent);
        executorService.execute(() -> {
            try {
                pushMessageInternel(pushMessage, deviceId, pushContent);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        });
    }

    private void pushMessageInternel(PushMessage pushMessage, String deviceId, String pushContent) {
        System.out.println("pushMessageInternel");
        LOG.info("pushMessageInternel方法被调用!");
        if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_NORMAL && StringUtil.isNullOrEmpty(pushContent)) {
            LOG.info("推送内容为空, deviceId {}", deviceId);
            return;
        }

        MemorySessionStore.Session session = sessionsStore.getSession(deviceId);
        int badge = session.getUnReceivedMsgs();
        if (badge <= 0) {
            badge = 1;
        }
      /*  if (StringUtil.isNullOrEmpty(session.getDeviceToken())) {
            LOG.warn("Device token is empty for device {}", deviceId);
            return;
        }*/

        pushMessage.packageName = session.getAppName();
        pushMessage.pushType = session.getPushType();
        pushMessage.pushContent = pushContent;
        //  pushMessage.deviceToken = session.getDeviceToken();
        pushMessage.unReceivedMsg = badge;
        if (session.getPlatform() == ProtoConstants.Platform.Platform_iOS || session.getPlatform() == ProtoConstants.Platform.Platform_Android) {
            String url = androidPushServerUrl;
            if (session.getPlatform() == ProtoConstants.Platform.Platform_iOS) {
                url = iOSPushServerUrl;
                pushMessage.voipDeviceToken = session.getVoipDeviceToken();
            }
            // HttpUtils.httpJsonPost(url, new Gson().toJson(pushMessage, pushMessage.getClass()));
            LOG.info("pushMessageInternel push消息推送地址:{} , 推送数据:{}", notifyUrl, new Gson().toJson(pushMessage, pushMessage.getClass()));
            HttpUtils.httpJsonPost(notifyUrl, new Gson().toJson(pushMessage, pushMessage.getClass()));
            LOG.info("pushMessageInternel方法推送消息成功! 地址:{},推送消息:{}", notifyUrl, new Gson().toJson(pushMessage, pushMessage.getClass()));
        } else {
            LOG.info("非移动平台 {}", session.getPlatform());
        }
    }
}

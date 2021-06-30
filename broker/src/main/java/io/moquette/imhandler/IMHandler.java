/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.secret.util.ProtoUtil;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.google.protobuf.GeneratedMessage;
import com.xiaoleilu.hutool.util.DateUtil;
import io.moquette.server.Server;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.MessagesPublisher;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.liuyan.im.RateLimiter;
import com.liuyan.im.Utility;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_OVER_FREQUENCY;
import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

/**
 * 请求处理接口<br>
 * 当用户请求某个Topic，则调用相应Handler的handle方法
 */

abstract public class IMHandler<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(IMHandler.class);
    protected static IMessagesStore m_messagesStore = null;
    protected static ISessionsStore m_sessionsStore = null;
    protected static Server mServer = null;
    protected static MessagesPublisher publisher;
    private static ThreadPoolExecutorWrapper m_imBusinessExecutor;
    private static final RateLimiter mLimitCounter = new RateLimiter(5, 100);
    private Method parseDataMethod;
    private Class dataCls;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ActionMethod {
    }

    protected static String actionName;

    public static MessagesPublisher getPublisher() {
        return publisher;
    }

    public IMHandler() {
        try {
            if (StringUtil.isNullOrEmpty(actionName)) {
                Class cls = getClass();
                while (cls.getSuperclass() != null) {
                    for (Method method : cls.getSuperclass().getDeclaredMethods()) {
                        if (method.getAnnotation(ActionMethod.class) != null) {
                            actionName = method.getName();
                            break;
                        }
                    }
                    if (StringUtil.isNullOrEmpty(actionName)) {
                        cls = cls.getSuperclass();
                    } else {
                        break;
                    }
                }
            }

            Type t = getClass().getGenericSuperclass();
            ParameterizedType p = (ParameterizedType) t;
            Class<T> c = (Class<T>) p.getActualTypeArguments()[0];
            dataCls = c;

            if (dataCls.getSuperclass().equals(GeneratedMessage.class)) {
                parseDataMethod = dataCls.getMethod("parseFrom", byte[].class);
                if (!(this instanceof GetTokenHandler)) {
                    parseDataMethod = null;
                }
            } else if (dataCls.isPrimitive()) {

            } else {

            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    public T getDataObject(byte[] bytes) throws IllegalAccessException, InvocationTargetException {
        if (parseDataMethod != null) {
            T object = (T) parseDataMethod.invoke(dataCls, bytes);
            return object;
        }

        if (dataCls == String.class) {
            String str = new String(bytes);
            return (T) str;
        }

        if (dataCls == Byte.class) {
            Byte b = bytes[0];
            return (T) b;
        }

        if (dataCls == Void.class) {
            return null;
        }


        //目前还没有需求传int的参数，这里先注释掉
        //在需要使用时，需要注意大小端的问题。
        //这里使用的示例代码是小端的，注意一定要与协议中保持一直！！！
//        if (dataCls == Integer.class) {
//            int i = bytes[0];
//            for(int index = 0; index <8; index++) {
//                i <<= 1;
//                i += bytes[index];
//            }
//            Integer object = i;
//            return (T)object;
//         }

        //json ?
//        return (T)(new Gson().fromJson(new String(bytes), dataCls));
        return (T) (ProtoUtil.toProto(dataCls, new String(bytes)));
    }

    public static void init(IMessagesStore ms, ISessionsStore ss, MessagesPublisher p, ThreadPoolExecutorWrapper businessExecutor, Server server) {
        m_messagesStore = ms;
        m_sessionsStore = ss;
        publisher = p;
        m_imBusinessExecutor = businessExecutor;
        mServer = server;
    }


    public ErrorCode preAction(String clientID, String fromUser, String topic, Qos1PublishHandler.IMCallback callback) {
        LOG.info("imHandler fromUser={}, clientId={}, topic={}", fromUser, clientID, topic);
        if (!mLimitCounter.isGranted(clientID + fromUser + topic)) {
            ByteBuf ackPayload = Unpooled.buffer();
            ackPayload.ensureWritable(1).writeByte(ERROR_CODE_OVER_FREQUENCY.getCode());
            try {
                callback.onIMHandled(ERROR_CODE_OVER_FREQUENCY, ackPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    public void doHandler(String clientID, String fromUser, String topic, byte[] payloadContent, Qos1PublishHandler.IMCallback callback, boolean isAdmin) {
        m_imBusinessExecutor.execute(() -> {
            Qos1PublishHandler.IMCallback callbackWrapper = (errorCode, ackPayload) -> {
                callback.onIMHandled(errorCode, ackPayload);
                afterAction(clientID, fromUser, topic, callback);
            };

            ErrorCode preActionCode = preAction(clientID, fromUser, topic, callbackWrapper);

            if (preActionCode == ErrorCode.ERROR_CODE_SUCCESS) {
                ByteBuf ackPayload = Unpooled.buffer(1);
                ErrorCode errorCode = ERROR_CODE_SUCCESS;
                ackPayload.ensureWritable(1).writeByte(errorCode.getCode());

                try {
                    LOG.info("请求时间:{}    请求体:{}   请求用户:{}   请求topic:{}  请求者clientId:{}", DateUtil.now(), getDataObject(payloadContent).toString(), fromUser, topic, clientID);
                    errorCode = action(ackPayload, clientID, fromUser, isAdmin, getDataObject(payloadContent), callbackWrapper);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    errorCode = ErrorCode.ERROR_CODE_INVALID_DATA;
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    errorCode = ErrorCode.ERROR_CODE_SERVER_ERROR;
                }

                response(ackPayload, errorCode, callback);
                LOG.info("LIUYAN_ doHandler ErrorCode 最终返回:{}", errorCode);
            } else {
                LOG.error("Handler {} preAction failure", this.getClass().getName());
                ByteBuf ackPayload = Unpooled.buffer(1);
                ackPayload.ensureWritable(1).writeByte(preActionCode.getCode());
                response(ackPayload, preActionCode, callback);
            }
        });
    }

    /**
     * lambda:
     * Qos1PublishHandler.IMCallback callbackWrapper = new Qos1PublishHandler.IMCallback() {
     *
     * @Override public void onIMHandled(ErrorCode errorCode, ByteBuf ackPayload) {
     * callback.onIMHandled(errorCode, ackPayload);
     * afterAction(clientID, fromUser, topic, callback);
     * }
     * };
     */
    public void doClsterHandler(String clientID, String fromUser, String topic, byte[] payloadContent, Qos1PublishHandler.IMCallback callback, boolean isAdmin) {
        m_imBusinessExecutor.execute(() -> {
            Qos1PublishHandler.IMCallback callbackWrapper = (errorCode, ackPayload) -> {
                callback.onIMHandled(errorCode, ackPayload);
                afterAction(clientID, fromUser, topic, callback);
            };

            ErrorCode preActionCode = preAction(clientID, fromUser, topic, callbackWrapper);

            if (preActionCode == ErrorCode.ERROR_CODE_SUCCESS) {
                ByteBuf ackPayload = Unpooled.buffer(1);
                ErrorCode errorCode = ERROR_CODE_SUCCESS;
                ackPayload.ensureWritable(1).writeByte(errorCode.getCode());

                try {
                    LOG.info("(只转发不处理)集群处理请求时间:{}    请求体:{}   请求用户:{}   请求topic:{}  请求者clientId:{}", DateUtil.now(), getDataObject(payloadContent).toString(), fromUser, topic, clientID);
                    errorCode = action(ackPayload, clientID, fromUser, isAdmin, getDataObject(payloadContent), callbackWrapper);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    errorCode = ErrorCode.ERROR_CODE_INVALID_DATA;
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    errorCode = ErrorCode.ERROR_CODE_SERVER_ERROR;
                }

                response(ackPayload, errorCode, callback);
                LOG.info("LIUYAN_ 集群handler ErrorCode 最终返回:{}", errorCode);
            } else {
                LOG.error("Handler {} preAction failure", this.getClass().getName());
                ByteBuf ackPayload = Unpooled.buffer(1);
                ackPayload.ensureWritable(1).writeByte(preActionCode.getCode());
                response(ackPayload, preActionCode, callback);
            }
        });
    }

    private void response(ByteBuf ackPayload, ErrorCode errorCode, Qos1PublishHandler.IMCallback callback) {
        ackPayload.setByte(0, errorCode.getCode());
        try {
            callback.onIMHandled(errorCode, ackPayload);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }


    @ActionMethod
    abstract public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, T request, Qos1PublishHandler.IMCallback callback);

    public ErrorCode receiveAction(ByteBuf ackPayload, String clientID, String fromUser, T request) {
        ErrorCode errorCode = action(ackPayload, clientID, fromUser, false, request, null);
        return errorCode;
    }

    public void afterAction(String clientID, String fromUser, String topic, Qos1PublishHandler.IMCallback callback) {

    }

    protected long publish(String username, String clientID, WFCMessage.Message message) {
        Set<String> notifyReceivers = new LinkedHashSet<>();

        WFCMessage.Message.Builder messageBuilder = message.toBuilder();
        int pullType = m_messagesStore.getNotifyReceivers(username, messageBuilder, notifyReceivers, false);
        mServer.getImBusinessScheduler().execute(() -> this.publisher.publish2Receivers(messageBuilder.build(), notifyReceivers, clientID, pullType));
        return notifyReceivers.size();
    }

    /**
     * 这个方法很重要 是所有发消息的入口 建议维护im的同学仔细研究一下这个接口
     * <p>
     *
     * @param username  发送人id
     * @param clientID
     * @param message   消息对象
     * @param ignoreMsg
     * @return
     */
    protected long saveAndPublish(String username, String clientID, WFCMessage.Message message, boolean ignoreMsg) {
        Set<String> notifyReceivers = new LinkedHashSet<>();
        message = m_messagesStore.storeMessage(username, clientID, message);
        WFCMessage.Message.Builder messageBuilder = message.toBuilder();
        int pullType = m_messagesStore.getNotifyReceivers(username, messageBuilder, notifyReceivers, ignoreMsg);
        mServer.getImBusinessScheduler().execute(() -> this.publisher.publish2Receivers(messageBuilder.build(), notifyReceivers, clientID, pullType));
        return notifyReceivers.size();
    }

    protected long saveAndBroadcast(String username, String clientID, WFCMessage.Message message) {
        Set<String> notifyReceivers = m_messagesStore.getAllEnds();
        WFCMessage.Message updatedMessage = m_messagesStore.storeMessage(username, clientID, message);
        mServer.getImBusinessScheduler().execute(() -> publisher.publish2Receivers(updatedMessage, notifyReceivers, clientID, ProtoConstants.PullType.Pull_Normal));
        return notifyReceivers.size();
    }

    protected long saveAndMulticast(String username, String clientID, WFCMessage.Message message, Collection<String> targets) {
        Set<String> notifyReceivers = new HashSet<>();
        notifyReceivers.addAll(targets);
        WFCMessage.Message updatedMessage = m_messagesStore.storeMessage(username, clientID, message);
        mServer.getImBusinessScheduler().execute(() -> publisher.publish2Receivers(updatedMessage, notifyReceivers, clientID, ProtoConstants.PullType.Pull_Normal));
        return notifyReceivers.size();
    }

    public long receiveAndPublish(String username, String clientID, WFCMessage.Message message) {
        Set<String> notifyReceivers = new LinkedHashSet<>();
        WFCMessage.Message.Builder messageBuilder = message.toBuilder();
        int pullType = m_messagesStore.getNotifyReceivers(username, messageBuilder, notifyReceivers, false);
        mServer.getImBusinessScheduler().execute(() -> this.publisher.publish2ReceiversNew(messageBuilder.build(), notifyReceivers, clientID, pullType));
        return notifyReceivers.size();
    }
}

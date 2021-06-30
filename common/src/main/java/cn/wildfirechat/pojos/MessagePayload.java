/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import cn.wildfirechat.proto.WFCMessage;
import com.google.protobuf.ByteString;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 在网络传输中，消息内容会转化为消息负载,接收到消息负载后会转化为消息内容,消息内容有一个共同的抽象基类 {@link WFCMessage.MessageContent} 各种消息类型都派生与此基类。
 * 消息内容需要在网络上传输或本地存储，而消息内容是复杂多变的。因此消息发送过程中先encode为消息负载；消息接收后由消息负载decode为对应的消息内容。在数据库从存取也是如此。
 * 对于IM服务来说，所有收到的内容都是MessagePayload，发出去的也是MessagePayload
 */
public class MessagePayload {
    private int type;

    /**
     * 可搜索内容，用于本地搜索或者在服务器搜索  更改后内容大数据做审核用
     */
    private String searchableContent;

    /**
     * 对于自定义消息，如果需要推送需要encode此字段。推送内容会使用此字段。此字段会显示在用户手机推送内容区。
     * 如果需要推送，请在这个字段中填上需要推送的内容。
     */
    private String pushContent;
    private String content;
    private String base64edData;

    /**
     * 媒体类型，媒体消息内容使用，用来区别在服务器端文件对应的bucket。
     */
    private int mediaType;
    private String remoteMediaUrl;
    private int persistFlag;
    private int expireDuration;

    /**
     * 提醒类型（就是@某人或@全体）。0 不提醒；1 对mentionedTargets里的user进行提醒；2 对群内所有人提醒。
     */
    private int mentionedType;
    private List<String> mentionedTarget;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSearchableContent() {
        return searchableContent;
    }

    public void setSearchableContent(String searchableContent) {
        this.searchableContent = searchableContent;
    }

    public String getPushContent() {
        return pushContent;
    }

    public void setPushContent(String pushContent) {
        this.pushContent = pushContent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBase64edData() {
        return base64edData;
    }

    public void setBase64edData(String base64edData) {
        this.base64edData = base64edData;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public String getRemoteMediaUrl() {
        return remoteMediaUrl;
    }

    public void setRemoteMediaUrl(String remoteMediaUrl) {
        this.remoteMediaUrl = remoteMediaUrl;
    }

    public int getPersistFlag() {
        return persistFlag;
    }

    public void setPersistFlag(int persistFlag) {
        this.persistFlag = persistFlag;
    }

    public int getExpireDuration() {
        return expireDuration;
    }

    public void setExpireDuration(int expireDuration) {
        this.expireDuration = expireDuration;
    }

    public int getMentionedType() {
        return mentionedType;
    }

    public void setMentionedType(int mentionedType) {
        this.mentionedType = mentionedType;
    }

    public List<String> getMentionedTarget() {
        return mentionedTarget;
    }

    public void setMentionedTarget(List<String> mentionedTarget) {
        this.mentionedTarget = mentionedTarget;
    }

    public WFCMessage.MessageContent toProtoMessageContent() {
        WFCMessage.MessageContent.Builder builder = WFCMessage.MessageContent.newBuilder()
            .setType(type)
            .setMediaType(mediaType)
            .setPersistFlag(persistFlag)
            .setExpireDuration(expireDuration)
            .setMentionedType(mentionedType);

        if (!StringUtil.isNullOrEmpty(searchableContent))
            builder.setSearchableContent(searchableContent);
        if (!StringUtil.isNullOrEmpty(pushContent))
            builder.setPushContent(pushContent);
        if (!StringUtil.isNullOrEmpty(content))
            builder.setContent(content);
        if (!StringUtil.isNullOrEmpty(base64edData))
            builder.setData(ByteString.copyFrom(Base64.getDecoder().decode(base64edData)));
        if (!StringUtil.isNullOrEmpty(remoteMediaUrl))
            builder.setRemoteMediaUrl(remoteMediaUrl);
        if (mentionedTarget != null && mentionedTarget.size() > 0)
            builder.addAllMentionedTarget(mentionedTarget);

        return builder.build();
    }

    public static MessagePayload fromProtoMessageContent(WFCMessage.MessageContent protoContent) {
        if (protoContent == null)
            return null;

        MessagePayload payload = new MessagePayload();
        payload.type = protoContent.getType();
        payload.searchableContent = protoContent.getSearchableContent();
        payload.pushContent = protoContent.getPushContent();
        payload.content = protoContent.getContent();
        if (protoContent.getData() != null && protoContent.getData().size() > 0)
            payload.base64edData = Base64.getEncoder().encodeToString(protoContent.getData().toByteArray());
        payload.mediaType = protoContent.getMediaType();
        payload.remoteMediaUrl = protoContent.getRemoteMediaUrl();
        payload.persistFlag = protoContent.getPersistFlag();
        payload.expireDuration = protoContent.getExpireDuration();
        payload.mentionedType = protoContent.getMentionedType();
        payload.mentionedTarget = new ArrayList<>();
        payload.mentionedTarget.addAll(protoContent.getMentionedTargetList());
        return payload;
    }
}

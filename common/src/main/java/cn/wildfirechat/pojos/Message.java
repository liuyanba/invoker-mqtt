package cn.wildfirechat.pojos;


/**
 * 一个{@link Conversation}会话对应着多个message消息
 */
public class Message {
    /**
     * 所属的会话
     */
    private ConversationJson conversation;

    /**
     * 消息的内容，消息内容可以是多种格式，比如图片/文本/语音/地理位置等。
     */
    private ContentJson content;

    /**
     * 发送者
     */
    private String fromUser;

    /**
     * 消息ID
     * 如果消息内容是存储类型的，messageId对应于本地数据库中的自增id，
     * 同一条消息在发送方和接收方都可能是不同的，甚至在多端的情况下也不能保证相同。如果消息内容是非存储的，messageId为0.
     */
    private String messageId;

    /**
     * 消息在服务器处理的时间戳
     */
    private String serverTimestamp;


    public ConversationJson getConversation() {
        return conversation;
    }

    public void setConversation(ConversationJson conversation) {
        this.conversation = conversation;
    }

    public ContentJson getContent() {
        return content;
    }

    public void setContent(ContentJson content) {
        this.content = content;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(String serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}

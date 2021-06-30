package cn.wildfirechat.pojos;

public class SendMessageJson {
    private ContentJson content;
    private String fromUser;
    private ConversationJson conversation;

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

    public ConversationJson getConversation() {
        return conversation;
    }

    public void setConversation(ConversationJson conversation) {
        this.conversation = conversation;
    }
}

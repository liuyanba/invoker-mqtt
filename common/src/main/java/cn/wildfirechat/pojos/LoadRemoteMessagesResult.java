package cn.wildfirechat.pojos;

import java.util.List;

public class LoadRemoteMessagesResult {
    private List<Message> message;
    private String current;
    private String head;

    public List<Message> getMessage() {
        return message;
    }

    public void setMessage(List<Message> message) {
        this.message = message;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }
}

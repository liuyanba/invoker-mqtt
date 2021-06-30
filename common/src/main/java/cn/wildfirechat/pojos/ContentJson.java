package cn.wildfirechat.pojos;

public class ContentJson {

    private int type;
    private String content;
    private String searchableContent;
    private int persistFlag;


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSearchableContent() {
        return searchableContent;
    }

    public void setSearchableContent(String searchableContent) {
        this.searchableContent = searchableContent;
    }

    public int getPersistFlag() {
        return persistFlag;
    }

    public void setPersistFlag(int persistFlag) {
        this.persistFlag = persistFlag;
    }
}

/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class LoadRemoteMessagesRequest {

    private ConversationJson conversation;
    private Long beforeUid;
    private int count;
    private String fromUserCode;

    public ConversationJson getConversation() {
        return conversation;
    }

    public void setConversation(ConversationJson conversation) {
        this.conversation = conversation;
    }

    public Long getBeforeUid() {
        return beforeUid;
    }

    public void setBeforeUid(Long beforeUid) {
        this.beforeUid = beforeUid;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getFromUserCode() {
        return fromUserCode;
    }

    public void setFromUserCode(String fromUserCode) {
        this.fromUserCode = fromUserCode;
    }
}

/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

/**
 * 会话(Conversation)是指与特定对象聊天消息(Message)的集合，概念与微信中的会话一致，一条一条显示在消息界面中。
 */
public class Conversation {

    /**详情见 {@link cn.wildfirechat.proto.ProtoConstants.ConversationType}*/
    private int type;
    /**
     * 跟会话类型不同而不同，Single类型时，目标为对方用户Id；Group类型时，目标为群Id；Chatroom类型时，目标为聊天室Id；Channel类型时 目标为Channel ID。
     */
    private String target;

    /**
     * 会话线路可以更加方便的过滤会话。比如同一个app中不同部门可以使用不同的line区分。或者可以设计不同的场景使用不同的line等。如果没有这种特殊需求，使用0值就可以。
     */
    private int line;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}

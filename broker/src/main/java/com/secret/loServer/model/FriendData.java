/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.secret.loServer.model;

import java.io.Serializable;

public class FriendData implements Serializable {
    private String userId;
    private String friendUid;
    private String alias;
    private String extra;
    private int state;
    private int blacked;
    private long timestamp;


    public FriendData(String userId, String friendUid, String alias, String extra, int state, int blacked, long timestamp) {
        this.userId = userId;
        this.friendUid = friendUid;
        this.alias = alias;
        this.extra = extra;
        this.state = state;
        this.blacked = blacked;
        this.timestamp = timestamp;
    }

    public FriendData() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFriendUid() {
        return friendUid;
    }

    public void setFriendUid(String friendUid) {
        this.friendUid = friendUid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getBlacked() {
        return blacked;
    }

    public void setBlacked(int blacked) {
        this.blacked = blacked;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

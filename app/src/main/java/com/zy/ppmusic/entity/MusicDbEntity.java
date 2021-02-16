package com.zy.ppmusic.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Keep;

/**
 * 记录上次播放媒体信息
 * @author stealfeam
 */
@Entity
@Keep
public class MusicDbEntity {
    /**
     * 媒体的唯一id
     */
    @Id
    private String lastMediaId;
    /**
     * 上次播放媒体名称
     */
    private String lastPlayName;
    /**
     * 该媒体处于媒体库列表中的位置
     */
    private int lastPlayIndex;
    /**
     * 媒体上次播放的位置
     */
    private long lastPlayedPosition;
    /**
     * 媒体的作者
     */
    private String lastPlayAuthor;

    /**
     * 媒体的路径
     */
    private String lastMediaPath;
    @Generated(hash = 1362749471)
    public MusicDbEntity(String lastMediaId, String lastPlayName, int lastPlayIndex,
            long lastPlayedPosition, String lastPlayAuthor, String lastMediaPath) {
        this.lastMediaId = lastMediaId;
        this.lastPlayName = lastPlayName;
        this.lastPlayIndex = lastPlayIndex;
        this.lastPlayedPosition = lastPlayedPosition;
        this.lastPlayAuthor = lastPlayAuthor;
        this.lastMediaPath = lastMediaPath;
    }
    public MusicDbEntity() {
    }
    public String getLastMediaId() {
        return this.lastMediaId;
    }
    public void setLastMediaId(String lastMediaId) {
        this.lastMediaId = lastMediaId;
    }
    public String getLastPlayName() {
        return this.lastPlayName;
    }
    public void setLastPlayName(String lastPlayName) {
        this.lastPlayName = lastPlayName;
    }
    public int getLastPlayIndex() {
        return this.lastPlayIndex;
    }
    public void setLastPlayIndex(int lastPlayIndex) {
        this.lastPlayIndex = lastPlayIndex;
    }
    public long getLastPlayedPosition() {
        return this.lastPlayedPosition;
    }
    public void setLastPlayedPosition(long lastPlayedPosition) {
        this.lastPlayedPosition = lastPlayedPosition;
    }
    public String getLastPlayAuthor() {
        return this.lastPlayAuthor;
    }
    public void setLastPlayAuthor(String lastPlayAuthor) {
        this.lastPlayAuthor = lastPlayAuthor;
    }
    public String getLastMediaPath() {
        return this.lastMediaPath;
    }
    public void setLastMediaPath(String lastMediaPath) {
        this.lastMediaPath = lastMediaPath;
    }

    @Override
    public String toString() {
        return "MusicDbEntity{" +
                "lastMediaId='" + lastMediaId + '\'' +
                ", lastPlayName='" + lastPlayName + '\'' +
                ", lastPlayIndex=" + lastPlayIndex +
                ", lastPlayedPosition=" + lastPlayedPosition +
                ", lastPlayAuthor='" + lastPlayAuthor + '\'' +
                ", lastMediaPath='" + lastMediaPath + '\'' +
                '}';
    }
}

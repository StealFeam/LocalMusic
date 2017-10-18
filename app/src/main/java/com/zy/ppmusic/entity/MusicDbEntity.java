package com.zy.ppmusic.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 记录上次播放媒体信息
 */
@Entity
public class MusicDbEntity {
    @Id
    private String lastMediaId;//媒体的唯一id
    private String lastPlayName;//上次播放媒体名称
    private int lastPlayIndex;//该媒体处于媒体库列表中的位置
    private int lastPlayedPosition;//媒体上次播放的位置
    private String lastPlayAuthor;//媒体的作者
    private String lastMediaPath;//媒体的路径
    @Generated(hash = 702547690)
    public MusicDbEntity(String lastMediaId, String lastPlayName, int lastPlayIndex,
            int lastPlayedPosition, String lastPlayAuthor, String lastMediaPath) {
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
    public int getLastPlayedPosition() {
        return this.lastPlayedPosition;
    }
    public void setLastPlayedPosition(int lastPlayedPosition) {
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

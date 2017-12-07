package com.zy.ppmusic.entity;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author ZhiTouPC
 */
public class MusicInfoEntity implements Serializable{
    private static final long serialVersionUID = -1016970004377532215L;
    private String mediaId;
    private String musicName;
    private String artist;
    private String queryPath;
    /**
     * 文件大小
     */
    private long size;
    /**
     * 时长
     */
    private long duration;
    /**
     * 专辑图片数据
     */
    private byte[] iconData;

    public MusicInfoEntity() {
    }

    public MusicInfoEntity(String mediaId, String musicName, String artist, String queryPath, long size, long duration, byte[] iconData) {
        this.mediaId = mediaId;
        this.musicName = musicName;
        this.artist = artist;
        this.queryPath = queryPath;
        this.size = size;
        this.duration = duration;
        this.iconData = iconData;
    }

    public byte[] getIconData() {
        return iconData;
    }

    public void setIconData(byte[] iconData) {
        this.iconData = iconData;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getQueryPath() {
        return queryPath;
    }

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    @Override
    public String toString() {
        return "MusicInfoEntity{" +
                "mediaId='" + mediaId + '\'' +
                ", musicName='" + musicName + '\'' +
                ", artist='" + artist + '\'' +
                ", queryPath='" + queryPath + '\'' +
                ", size=" + size +
                ", duration=" + duration +
                ", iconData=" + Arrays.toString(iconData) +
                '}';
    }


}

package com.zy.ppmusic;

import android.os.Parcel;
import android.os.Parcelable;

public class MusicInfoEntity implements Parcelable{
    private String name;
    private String path;
    private int length;
    private int currentPlayPosition;//当前播放的位置
    private int indexOfArray;//在list中的位置

    public MusicInfoEntity(){

    }

    public MusicInfoEntity(String name, int length, String path, int currentPlayPosition,int indexOfArray) {
        this.name = name;
        this.length = length;
        this.path = path;
        this.indexOfArray = indexOfArray;
        this.currentPlayPosition = currentPlayPosition;
    }

    protected MusicInfoEntity(Parcel in) {
        name = in.readString();
        path = in.readString();
        length = in.readInt();
        currentPlayPosition = in.readInt();
        indexOfArray = in.readInt();
    }

    public static final Creator<MusicInfoEntity> CREATOR = new Creator<MusicInfoEntity>() {
        @Override
        public MusicInfoEntity createFromParcel(Parcel in) {
            return new MusicInfoEntity(in);
        }

        @Override
        public MusicInfoEntity[] newArray(int size) {
            return new MusicInfoEntity[size];
        }
    };

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getCurrentPlayPosition() {
        return currentPlayPosition;
    }

    public void setCurrentPlayPosition(int currentPlayPosition) {
        this.currentPlayPosition = currentPlayPosition;
    }

    public int getIndexOfArray() {
        return indexOfArray;
    }

    public void setIndexOfArray(int indexOfArray) {
        this.indexOfArray = indexOfArray;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(path);
        dest.writeInt(length);
        dest.writeInt(currentPlayPosition);
        dest.writeInt(indexOfArray);
    }

    @Override
    public String toString() {
        return "MusicInfoEntity{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", length=" + length +
                ", currentPlayPosition=" + currentPlayPosition +
                ", indexOfArray=" + indexOfArray +
                '}';
    }
}

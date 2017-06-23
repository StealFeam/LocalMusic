package com.zy.ppmusic.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MusicInfoEntity implements Parcelable{
    private String name;
    private int length;

    public MusicInfoEntity(){

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

    @Override
    public String toString() {
        return "MusicInfoEntity{" +
                "name='" + name + '\'' +
                ", length='" + length + '\'' +
                '}';
    }

    protected MusicInfoEntity(Parcel in) {
        name = in.readString();
        length = in.readInt();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(length);
    }
}

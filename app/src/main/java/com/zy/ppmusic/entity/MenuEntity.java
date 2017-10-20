package com.zy.ppmusic.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author ZhiTouPC
 */
public class MenuEntity implements Parcelable{
    private int icon;
    private String title;

    public MenuEntity(int icon, String title) {
        this.icon = icon;
        this.title = title;
    }

    protected MenuEntity(Parcel in) {
        icon = in.readInt();
        title = in.readString();
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static final Creator<MenuEntity> CREATOR = new Creator<MenuEntity>() {
        @Override
        public MenuEntity createFromParcel(Parcel in) {
            return new MenuEntity(in);
        }

        @Override
        public MenuEntity[] newArray(int size) {
            return new MenuEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(icon);
        dest.writeString(title);
    }
}

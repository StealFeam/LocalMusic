package com.zy.ppmusic.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MainMenuEntity implements Parcelable{
    private String menuTitle;
    private int menuRes;

    public MainMenuEntity() {
    }

    public MainMenuEntity(String menuTitle, int menuRes) {
        this.menuTitle = menuTitle;
        this.menuRes = menuRes;
    }

    protected MainMenuEntity(Parcel in) {
        menuTitle = in.readString();
        menuRes = in.readInt();
    }

    public static final Creator<MainMenuEntity> CREATOR = new Creator<MainMenuEntity>() {
        @Override
        public MainMenuEntity createFromParcel(Parcel in) {
            return new MainMenuEntity(in);
        }

        @Override
        public MainMenuEntity[] newArray(int size) {
            return new MainMenuEntity[size];
        }
    };

    public String getMenuTitle() {
        return menuTitle;
    }

    public void setMenuTitle(String menuTitle) {
        this.menuTitle = menuTitle;
    }

    public int getMenuRes() {
        return menuRes;
    }

    public void setMenuRes(int menuRes) {
        this.menuRes = menuRes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(menuTitle);
        dest.writeInt(menuRes);
    }
}

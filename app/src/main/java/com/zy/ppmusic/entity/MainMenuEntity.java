package com.zy.ppmusic.entity;

public class MainMenuEntity {
    private String menuTitle;
    private int menuRes;

    public MainMenuEntity() {
    }

    public MainMenuEntity(String menuTitle, int menuRes) {
        this.menuTitle = menuTitle;
        this.menuRes = menuRes;
    }

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
}

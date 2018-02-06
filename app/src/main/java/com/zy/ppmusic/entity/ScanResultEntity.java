package com.zy.ppmusic.entity;


import android.bluetooth.BluetoothDevice;

/**
 * @author ZhiTouPC
 */
public class ScanResultEntity {
    /**
     * 布局的id
     */
    private int type;
    /**
     * 头布局的标题
     */
    private String title;
    private String state;
    private BluetoothDevice device;

    public ScanResultEntity() {
    }

    public ScanResultEntity(int type, String title) {
        this.type = type;
        this.title = title;
    }

    public ScanResultEntity(int type, BluetoothDevice device) {
        this.type = type;
        this.device = device;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "ScanResultEntity{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", state='" + state + '\'' +
                ", device=" + device +
                '}';
    }
}

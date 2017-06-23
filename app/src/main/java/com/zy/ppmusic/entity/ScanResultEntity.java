package com.zy.ppmusic.entity;

import android.bluetooth.BluetoothDevice;

public class ScanResultEntity {
    private int type;//布局的id
    private String title;//头布局的标题
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

    @Override
    public String toString() {
        return "ScanResultEntity{" +
                "type=" + type +
                ", device=" + device +
                '}';
    }
}

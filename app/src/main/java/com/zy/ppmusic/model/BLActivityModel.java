package com.zy.ppmusic.model;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.zy.ppmusic.R;
import com.zy.ppmusic.contract.IBLActivityContract;
import com.zy.ppmusic.entity.ScanResultEntity;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BLActivityModel implements IBLActivityContract.IModel{

    public BLActivityModel() {
    }

    @NotNull
    @Override
    public List<ScanResultEntity> getExitDevices(@NotNull BluetoothAdapter mBlueAdapter,@NotNull BluetoothA2dp mBlueA2dp) {
        List<ScanResultEntity> list = new ArrayList<>();
        Set<BluetoothDevice> bondedDevices = mBlueAdapter.getBondedDevices();
        for (BluetoothDevice bondedDevice : bondedDevices) {
            ScanResultEntity entity = new ScanResultEntity(R.layout.item_scan_child, bondedDevice);
            if(mBlueA2dp.getConnectedDevices().contains(bondedDevice)){
                entity.setState("已连接");
            }
            list.add(entity);
        }
        return list;
    }

    @Override
    public boolean connectDevice(@NotNull BluetoothDevice device,@NotNull BluetoothAdapter mBlueAdapter,@NotNull BluetoothA2dp mBlueA2dp) {
        if (mBlueAdapter.isDiscovering()) {
            mBlueAdapter.cancelDiscovery();
        }
        try {
            Method method = mBlueA2dp.getClass().getMethod("connect", BluetoothDevice.class);
            method.setAccessible(true);
            method.invoke(mBlueA2dp, device);
            return true;
        } catch (Exception e) {
            System.out.println("connect device error..." + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean disconnectDevice(@NotNull BluetoothDevice device,@NotNull BluetoothAdapter mBlueAdapter,@NotNull BluetoothA2dp mBlueA2dp) {
        try {
            Method method = mBlueA2dp.getClass().getMethod("disconnect", BluetoothDevice.class);
            method.setAccessible(true);
            method.invoke(mBlueA2dp, device);
            return true;
        } catch (Exception e) {
            System.out.println("connect device error..." + e.getMessage());
            return false;
        }
    }

}

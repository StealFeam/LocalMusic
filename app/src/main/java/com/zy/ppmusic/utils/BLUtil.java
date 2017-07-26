package com.zy.ppmusic.utils;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class BLUtil {
    private static final String TAG = "BLUtil";
    private List<BluetoothDevice> mConnectedDevices;

    private static class Inner{
        private static BLUtil instance = new BLUtil();
    }

    public static BLUtil getInstance(){
        return Inner.instance;
    }

    private BLUtil(){
    }

    public void connectDevice(BluetoothDevice device, BluetoothA2dp a2dp){
        Method method = null;
        try {
            method = a2dp.getClass().getMethod("connect", BluetoothDevice.class);
            method.setAccessible(true);
            method.invoke(a2dp, device);
            Log.w(TAG, "connectDevice: complete" );
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void disconnectDevice(){

    }

    public void createBond(){

    }

    public void destroyBond(){

    }

}

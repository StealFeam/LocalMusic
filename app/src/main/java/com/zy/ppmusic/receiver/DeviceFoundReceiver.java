package com.zy.ppmusic.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zy.ppmusic.bl.BlScanActivity;

import java.lang.ref.WeakReference;

public class DeviceFoundReceiver extends BroadcastReceiver{
    private WeakReference<BlScanActivity> weakReference;

    public DeviceFoundReceiver(BlScanActivity weakReference) {
        this.weakReference = new WeakReference<>(weakReference);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)){
            if(weakReference.get() != null){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                weakReference.get().foundNewDevice(device);
            }
        }
    }
}

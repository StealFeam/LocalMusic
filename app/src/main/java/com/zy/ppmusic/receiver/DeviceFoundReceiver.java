package com.zy.ppmusic.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.zy.ppmusic.mvp.view.BlScanActivity;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * @author ZhiTouPC
 */
public class DeviceFoundReceiver extends BroadcastReceiver{
    private static final String TAG = "DeviceFoundReceiver";
    private final WeakReference<BlScanActivity> weakReference;

    public DeviceFoundReceiver(BlScanActivity weakReference) {
        this.weakReference = new WeakReference<>(weakReference);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if(Objects.equals(intent.getAction(), BluetoothDevice.ACTION_FOUND)){
                if(weakReference.get() != null){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    weakReference.get().foundNewDevice(device);
                }
            }
        }else{
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                if(weakReference.get() != null){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    weakReference.get().foundNewDevice(device);
                }
            }
        }
    }
}

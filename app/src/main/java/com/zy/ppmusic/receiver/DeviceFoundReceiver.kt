package com.zy.ppmusic.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.zy.ppmusic.mvp.view.BlScanActivity
import java.lang.ref.WeakReference

/**
 * @author stealfeam
 */
class DeviceFoundReceiver(weakReference: BlScanActivity) : BroadcastReceiver() {
    private val weakReference: WeakReference<BlScanActivity> = WeakReference(weakReference)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() called with: context = [$context], intent = [$intent]")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                weakReference.get()?.foundNewDevice(device)
            }
        } else {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                weakReference.get()?.foundNewDevice(device)
            }
        }
    }

    companion object {
        private val TAG = "DeviceFoundReceiver"
    }
}

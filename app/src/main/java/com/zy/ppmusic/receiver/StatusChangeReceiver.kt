package com.zy.ppmusic.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zy.ppmusic.mvp.view.BlScanActivity
import java.lang.ref.WeakReference

/**
 * 监听蓝牙的状态修改
 */
class StatusChangeReceiver(ref: BlScanActivity) : BroadcastReceiver() {
    private val reference: WeakReference<BlScanActivity>? by lazy {
        WeakReference(ref)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                println("bl state changed....")
                val state = intent.extras?.getInt(BluetoothAdapter.EXTRA_STATE) ?: 0
                reference?.get()?.onBLStateChange(state)
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {//蓝牙配对广播
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                reference?.get()?.onDeviceBondStateChanged(bondState, device)
            }
            //连接状态广播
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
//                    val state = intent.extras.getInt(BluetoothA2dp.EXTRA_STATE)
//                    val device = intent.extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                reference?.get()?.connectStateChanged()
            }
            //扫描开始广播
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                reference?.get()?.discoveryStateChange(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            }
            //扫描结束广播
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                reference?.get()?.discoveryStateChange(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            else -> {
                println("other action=" + intent.action)
            }
        }
    }

}
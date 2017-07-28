package com.zy.ppmusic.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zy.ppmusic.bl.BlScanActivity
import java.lang.ref.WeakReference

/**
 * 监听蓝牙的状态修改
 */
class StatusChangeReceiver(ref:BlScanActivity): BroadcastReceiver() {
    private var reference:WeakReference<BlScanActivity>?=null
    init {
        reference = WeakReference(ref)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent!!.action == BluetoothAdapter.ACTION_STATE_CHANGED){
            if(reference!!.get() != null){
                println("bl state changed....")
                val state = intent.extras.getInt(BluetoothAdapter.EXTRA_STATE)
                reference!!.get()!!.onBLStateChange(state)
            }
        }else{
            when(intent.action){
                BluetoothDevice.ACTION_BOND_STATE_CHANGED->{
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    when (bondState) {
                        BluetoothDevice.BOND_BONDED-> {
                            println("Device:" + device.name + " bonded.")
                            reference!!.get()!!.mPresenter!!.connectDevice(device)
                        }
                        BluetoothDevice.BOND_BONDING -> println("Device:" + device.getName() + " bonding.....")
                        BluetoothDevice.BOND_NONE -> {
                            println("Device:" + device.getName() + " not bonded.")
                            //不知道是蓝牙耳机的关系还是什么原因，经常配对不成功
                            //配对不成功的话，重新尝试配对
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                device.createBond()
                            }
                        }
                        else -> {
                        }
                    }

                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED->{
                    val state = intent.extras.getInt(BluetoothA2dp.EXTRA_STATE)
                    val device = intent.extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    reference!!.get()!!.connectStateChanged(state,device)
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED->{
                    reference!!.get()!!.discoveryStateChange(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{
                    reference!!.get()!!.discoveryStateChange(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                else->{
                    println("other action="+intent.action)
                }
            }
        }

    }

}
package com.zy.ppmusic.contract

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.zy.ppmusic.base.IBaseModel
import com.zy.ppmusic.base.IBasePresenter
import com.zy.ppmusic.base.IBaseView
import com.zy.ppmusic.entity.ScanResultEntity

interface IBLActivityContract {
    interface IView : IBaseView {
        fun setExitsDevices(exitList: MutableList<ScanResultEntity>)
        fun getContext(): Context
    }

    interface IPresenter : IBasePresenter {
        fun getExitsDevices()
        fun isSupportBl(): Boolean
        fun connectDevice(device: BluetoothDevice): Boolean
        fun disconnectDevice(device: BluetoothDevice): Boolean
        fun isConnected(device: BluetoothDevice): Boolean
        fun getBondDevice(): Set<BluetoothDevice>?
        fun getConnectDevice(): List<BluetoothDevice>?
        fun getConnectState(device: BluetoothDevice):Int
        fun startDiscovery(): Boolean
        fun cancelDiscovery(): Boolean
        fun isEnable(): Boolean
        fun enable()
        fun disable()
        fun isDiscovering(): Boolean
    }

    interface IModel : IBaseModel {
        fun getExitsDevices(adapter: BluetoothAdapter, a2dp: BluetoothA2dp): MutableList<ScanResultEntity>
        fun connectDevice(device: BluetoothDevice, adapter: BluetoothAdapter, a2dp: BluetoothA2dp): Boolean
        fun disconnectDevice(device: BluetoothDevice, adapter: BluetoothAdapter, a2dp: BluetoothA2dp): Boolean
    }
}
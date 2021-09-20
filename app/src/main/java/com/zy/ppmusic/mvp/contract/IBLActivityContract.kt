package com.zy.ppmusic.mvp.contract

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.zy.ppmusic.entity.ScanResultEntity
import com.zy.ppmusic.mvp.base.AbstractBasePresenter
import com.zy.ppmusic.mvp.base.IBaseModel
import com.zy.ppmusic.mvp.base.IBaseView

interface IBLActivityContract {
    interface IBLActivityView : IBaseView {
        fun setExitsDevices(exitList: MutableList<ScanResultEntity>)
        fun getContext(): Context
    }

    abstract class AbstractBLActivityPresenter(v:IBLActivityView) : AbstractBasePresenter<IBLActivityView,
            IBLActivityModel>(v) {

        abstract fun getExitsDevices()
        abstract fun isSupportBl(): Boolean
        abstract fun connectDevice(device: BluetoothDevice): Boolean
        abstract fun disconnectDevice(device: BluetoothDevice): Boolean
        abstract fun isConnected(device: BluetoothDevice): Boolean
        abstract fun getBondDevice(): Set<BluetoothDevice>?
        abstract fun getConnectDevice(): List<BluetoothDevice>?
        abstract fun getConnectState(device: BluetoothDevice):Int
        abstract fun removeBondDevice(device: BluetoothDevice):Boolean
        abstract fun startDiscovery(): Boolean
        abstract fun cancelDiscovery(): Boolean
        abstract fun isEnable(): Boolean
        abstract fun enable()
        abstract fun disable()
        abstract fun isDiscovering(): Boolean
    }

    interface IBLActivityModel : IBaseModel {
        fun getExitsDevices(adapter: BluetoothAdapter, a2dp: BluetoothA2dp): MutableList<ScanResultEntity>
        fun connectDevice(device: BluetoothDevice, adapter: BluetoothAdapter, a2dp: BluetoothA2dp): Boolean
        fun disconnectDevice(device: BluetoothDevice, adapter: BluetoothAdapter, a2dp: BluetoothA2dp): Boolean
        fun removeBond(device: BluetoothDevice):Boolean
    }
}
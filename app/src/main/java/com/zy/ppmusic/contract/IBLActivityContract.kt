package com.zy.ppmusic.contract

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.zy.ppmusic.base.IBaseModel
import com.zy.ppmusic.base.IBasePresenter
import com.zy.ppmusic.base.IBaseView
import com.zy.ppmusic.entity.ScanResultEntity

interface IBLActivityContract{
    interface IView : IBaseView{
        fun setExitDevices(exitList: MutableList<ScanResultEntity>)
        fun getContext() : Context
    }

    interface IPresenter : IBasePresenter{
        fun getExitDevices()
        fun isSupportBl() : Boolean
        fun connectDevice(device: BluetoothDevice) :Boolean
        fun disconnectDevice(device: BluetoothDevice) : Boolean
        fun isConnected(device: BluetoothDevice) : Boolean
        fun getBondDevice() : List<BluetoothDevice>
        fun startDiscovery() : Boolean
        fun cancelDiscovery() : Boolean
        fun isEnable():Boolean
        fun enable()
        fun disable()
    }

    interface IModel : IBaseModel{
        fun getExitDevices(adapter: BluetoothAdapter,a2dp: BluetoothA2dp) : MutableList<ScanResultEntity>
        fun connectDevice(device: BluetoothDevice,adapter: BluetoothAdapter,a2dp: BluetoothA2dp) :Boolean
        fun disconnectDevice(device: BluetoothDevice,adapter: BluetoothAdapter,a2dp: BluetoothA2dp) : Boolean
    }
}
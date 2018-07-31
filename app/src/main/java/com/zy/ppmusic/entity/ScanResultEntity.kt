package com.zy.ppmusic.entity


import android.bluetooth.BluetoothDevice

/**
 * @author ZhiTouPC
 */
class ScanResultEntity {
    /**
     * 布局的id
     */
    var type: Int = 0
    /**
     * 头布局的标题
     */
    var title: String? = null
    var state: String? = null
    var device: BluetoothDevice? = null

    constructor() {}

    constructor(type: Int, title: String) {
        this.type = type
        this.title = title
    }

    constructor(type: Int, device: BluetoothDevice) {
        this.type = type
        this.device = device
    }

    override fun toString(): String {
        return "ScanResultEntity{" +
                "type=" + type +
                ", title='" + title + '\''.toString() +
                ", state='" + state + '\''.toString() +
                ", device=" + device +
                '}'.toString()
    }
}

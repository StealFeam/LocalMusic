package com.zy.ppmusic.bl

import android.app.Activity
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.ScanResultAdapter
import com.zy.ppmusic.contract.IBLActivityContract
import com.zy.ppmusic.entity.ScanResultEntity
import com.zy.ppmusic.presenter.BLActivityPresenter
import com.zy.ppmusic.receiver.DeviceFoundReceiver
import com.zy.ppmusic.receiver.StatusChangeReceiver
import com.zy.ppmusic.view.EasyTintView
import java.util.*

class BlScanActivity : AppCompatActivity(), IBLActivityContract.IView {
    private val TAG = "BlScanActivity"
    private val REQUEST_ENABLE_BL = 0x001
    private var sw_bl: SwitchCompat? = null
    private var blStateChangeReceiver: StatusChangeReceiver? = null
    private var mScanDeviceList: ArrayList<ScanResultEntity>? = null
    private var recyclerShowResult: RecyclerView? = null
    private var adapterShowResult: ScanResultAdapter? = null
    private var mDeviceFoundReceiver: DeviceFoundReceiver? = null
    var mPresenter: BLActivityPresenter? = null
    private var mToolBar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bl_scan)
        mToolBar = findViewById(R.id.toolbar_bl) as Toolbar
        setSupportActionBar(mToolBar)
        mPresenter = BLActivityPresenter(this)
        if (mPresenter!!.isSupportBl().not()) {
            showToast("设备不支持蓝牙")
            finish()
            return
        }

        mScanDeviceList = ArrayList()
        recyclerShowResult = findViewById(R.id.show_device_recycler) as RecyclerView
        sw_bl = findViewById(R.id.switch_bl) as SwitchCompat
        mToolBar!!.title = if (mPresenter!!.isEnable()) {
            sw_bl!!.isChecked = true
            mPresenter!!.getExitDevices()
            mPresenter!!.startDiscovery()
            "蓝牙已开启"
        } else {
            sw_bl!!.isChecked = false
            "蓝牙已关闭"
        }

        sw_bl!!.setOnCheckedChangeListener({ _, isChecked ->
            if (isChecked) {
                // 打开蓝牙
                if (mPresenter!!.isEnable().not()) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    // 设置蓝牙可见性，最多300秒
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    startActivityForResult(intent, REQUEST_ENABLE_BL)
                }
            } else {
                mPresenter!!.disable()
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun setExitDevices(exitList: MutableList<ScanResultEntity>) {
        mScanDeviceList!!.clear()
        if (exitList.size > 0) {
            mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_title, "已配对的设备"))
        }
        mScanDeviceList!!.addAll(mScanDeviceList!!.size, exitList)
        mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_title, "扫描到的设备"))

        if (adapterShowResult == null) {
            adapterShowResult = ScanResultAdapter(mScanDeviceList!!)
            adapterShowResult!!.setListener(object : ScanResultAdapter.OnItemClickListener() {
                override fun onItemClick(device: BluetoothDevice, position: Int) {
                    val deviceClass = device.bluetoothClass
                    //如果是音频类型的设备才能进行连接
                    if (deviceClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                            deviceClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) {
                        println("click called   name=" + device.name + ",position=$position")
                        if (!device.createBond()) {//如果返回值为false，说明设备已经配对过了直接连接设备
                            if (mPresenter!!.isConnected(device)) {
                                mPresenter!!.disconnectDevice(device)
                            } else {
                                mPresenter!!.connectDevice(device)
                            }
                        }
                    } else {
                        showToast("此设备不可作为蓝牙耳机")
                    }
                }
            })
            recyclerShowResult!!.adapter = adapterShowResult
            recyclerShowResult!!.layoutManager = LinearLayoutManager(this)
        } else {
            adapterShowResult!!.updateData(mScanDeviceList!!)
            adapterShowResult!!.notifyDataSetChanged()
        }
        mPresenter!!.startDiscovery()
    }

    private fun clearData() {
        adapterShowResult!!.clearData()
    }

    override fun getContext(): Context {
        return applicationContext
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bl_refresh, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_refresh -> {
                mPresenter!!.getExitDevices()
                mPresenter!!.cancelDiscovery()
                mPresenter!!.startDiscovery()
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 修改文字等其他信息
     */
    fun onBLStateChange(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_TURNING_ON -> {
                mToolBar!!.title = "蓝牙正在开启"
            }
            BluetoothAdapter.STATE_ON -> {
                mToolBar!!.title = "蓝牙已开启"
                sw_bl!!.isChecked = true
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                mToolBar!!.title = "蓝牙正在关闭"
            }
            BluetoothAdapter.STATE_OFF -> {
                mToolBar!!.title = "蓝牙已关闭"
                sw_bl!!.isChecked = false
                clearData()
            }
            else -> {
                println("收到其他状态...$state")
            }
        }
    }


    fun discoveryStateChange(state: String) {
        when (state) {
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                stopDiscovery()
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                startDiscovery()
            }
        }
    }

    var anim: RotateAnimation? = null

    private fun startDiscovery() {
        val v = findViewById(R.id.action_refresh)
        anim = RotateAnimation(0f, 360f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        anim!!.repeatCount = -1
        anim!!.interpolator = LinearInterpolator()
        anim!!.duration = 800
        if (v != null) {
            v.animation = anim
        }
        anim!!.start()
    }

    private fun stopDiscovery() {
        if (anim != null) {
            anim!!.cancel()
        }
    }

    fun connectStateChanged(state: Int, device: BluetoothDevice) {
        var stateStr = ""
        println("state=$state")
        when (state) {
            BluetoothA2dp.STATE_CONNECTING -> {
                println("正在连接")
                stateStr = "正在连接"
            }
            BluetoothA2dp.STATE_DISCONNECTED -> {
                println("断开连接")
                stateStr = "断开连接"
            }
            BluetoothA2dp.STATE_CONNECTED -> {
                println("已连接..." + device.name + ",address=" + device.address)
                stateStr = "已连接"
            }
        }
        if (mPresenter!!.getBondDevice().contains(device)) {
            val position = mPresenter!!.getBondDevice().indexOf(device)
            val scanResultEntity = mScanDeviceList!![(1 + position)]
            scanResultEntity.state = stateStr
            adapterShowResult!!.updateData(mScanDeviceList!!)
            adapterShowResult!!.updateChildData(position)
        }
    }

    override fun onResume() {
        super.onResume()
        if (blStateChangeReceiver == null) {
            blStateChangeReceiver = StatusChangeReceiver(this)
        }
        if (mDeviceFoundReceiver == null) {
            mDeviceFoundReceiver = DeviceFoundReceiver(this)
        }

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)

        registerReceiver(mDeviceFoundReceiver, foundFilter)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        registerReceiver(blStateChangeReceiver, filter)
    }

    /**
     *  找到新设备添加到列表中
     */
    fun foundNewDevice(device: BluetoothDevice) {
        println("新发现的设备。。。" + device.toString())
        if (!isInList(device)) {
            mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_child, device))
            println("此设备不存在列表。。。" + mScanDeviceList!!.size)
            adapterShowResult!!.updateData(mScanDeviceList!!)
            adapterShowResult!!.notifyDataSetChanged()
        } else println("已经存在了设备")
    }

    /**
     * 判断新发现的设备是否已经存在列表中
     */
    private fun isInList(device: BluetoothDevice): Boolean {
        mScanDeviceList!!.forEachIndexed { _, scanEntity ->
            if (scanEntity.device == device) {
                return true
            }
        }
        return false
    }

    /**
     * 删除设备
     */
    fun removeDevice(device: BluetoothDevice) {
        mScanDeviceList!!.forEachIndexed { _, scanResultEntity ->
            if (scanResultEntity.device == device) {
                mScanDeviceList!!.remove(scanResultEntity)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mDeviceFoundReceiver)
        unregisterReceiver(blStateChangeReceiver)
        mPresenter!!.cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.REQUEST_ENABLE_BL && resultCode == Activity.RESULT_OK) {
            showToast("蓝牙开启成功")
            mPresenter!!.getExitDevices()
            mPresenter!!.startDiscovery()
            onBLStateChange(BluetoothAdapter.STATE_ON)
        } else {
            showToast("蓝牙开启失败")
            onBLStateChange(BluetoothAdapter.STATE_OFF)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun showToast(msg: String) {
        EasyTintView.makeText(recyclerShowResult, msg, EasyTintView.TINT_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter!!.destroyView()
    }
}

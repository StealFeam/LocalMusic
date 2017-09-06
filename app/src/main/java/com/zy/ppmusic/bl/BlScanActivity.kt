package com.zy.ppmusic.bl

import android.app.Activity
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.ScanResultAdapter
import com.zy.ppmusic.contract.IBLActivityContract
import com.zy.ppmusic.entity.ScanResultEntity
import com.zy.ppmusic.presenter.BLActivityPresenter
import com.zy.ppmusic.receiver.DeviceFoundReceiver
import com.zy.ppmusic.receiver.StatusChangeReceiver
import com.zy.ppmusic.view.EasyTintView
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

/**
 * 1.点击配对未出现在已配对列表中
 * 2.状态没更新
 * 3.进入页面如果已经开启没有显示状态
 */
class BlScanActivity : AppCompatActivity(), IBLActivityContract.IView, EasyPermissions.PermissionCallbacks {
    private val REQUEST_ENABLE_BL = 0x001
    private var mBlueToothOpenSwitch: SwitchCompat? = null//蓝牙开关
    private var blStateChangeReceiver: StatusChangeReceiver? = null
    private var mScanDeviceList: ArrayList<ScanResultEntity>? = null
    private var recyclerShowResult: RecyclerView? = null
    private var adapterShowResult: ScanResultAdapter? = null
    private var mDeviceFoundReceiver: DeviceFoundReceiver? = null
    var mPresenter: BLActivityPresenter? = null
    private var mLoadingAnim: RotateAnimation? = null
    private var mToolBar: Toolbar? = null
    private var mRefreshScanMenu: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bl_scan)
        mToolBar = findViewById(R.id.toolbar_bl) as Toolbar

        mPresenter = BLActivityPresenter(this)
        recyclerShowResult = findViewById(R.id.show_device_recycler) as RecyclerView
        if (mPresenter!!.isSupportBl().not()) {
            Toast.makeText(this, "您的设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mScanDeviceList = ArrayList()
        mBlueToothOpenSwitch = findViewById(R.id.switch_bl) as SwitchCompat
        mToolBar!!.title = if (mPresenter!!.isEnable()) {
            mBlueToothOpenSwitch!!.isChecked = true
            mPresenter!!.getExitsDevices()
            mPresenter!!.startDiscovery()
            "蓝牙已开启"
        } else {
            mBlueToothOpenSwitch!!.isChecked = false
            "蓝牙已关闭"
        }
        setSupportActionBar(mToolBar)
        checkLocationPermission()
        mBlueToothOpenSwitch!!.setOnCheckedChangeListener({ _, isChecked ->
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

    private fun checkLocationPermission() {
        if (!EasyPermissions.hasPermissions(applicationContext, "android.permission.ACCESS_COARSE_LOCATION")) {
            println("没有权限")
            if (!EasyPermissions.permissionPermanentlyDenied(this, "android.permission.ACCESS_COARSE_LOCATION")) {
                EasyPermissions.requestPermissions(this@BlScanActivity, "获取粗略位置用来加快扫描",
                        1, "android.permission.ACCESS_COARSE_LOCATION")
            } else {
                val dialog = AppSettingsDialog.Builder(this)
                dialog.setRationale("没有位置信息将无法获取新设备，这是安卓6.0之后的系统要求，请允许权限")
                dialog.setNegativeButton("试一试", null)
                dialog.build().show()
                println("被关到小黑屋了")
            }
        } else {
            println("已经有权限了")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        println("权限通过了")
        if (requestCode == 1) {
            if (mPresenter!!.isEnable()) {
                mPresenter!!.startDiscovery()
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        if (requestCode == 1) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms!!)) {
                println("权限新特性")
                val dialog = AppSettingsDialog.Builder(this)
                dialog.setRationale("没有位置信息将无法获取新设备，这是安卓6.0之后的系统要求，请允许权限")
                dialog.build().show()
            } else {
                println("重新申请")
                EasyPermissions.requestPermissions(this@BlScanActivity, "获取粗略位置用来加快扫描,否则无法发现新设备",
                        1, "android.permission.ACCESS_COARSE_LOCATION")
            }
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun setExitsDevices(exitList: MutableList<ScanResultEntity>) {
        mScanDeviceList!!.clear()
        if (exitList.size > 0) {
            mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_title, "已配对的设备"))
        }
        mScanDeviceList!!.addAll(exitList)
        mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_title, "扫描到的设备"))

        if (mPresenter!!.getConnectDevice() != null) {
            mPresenter!!.getConnectDevice()!!.forEachIndexed { _, bluetoothDevice ->
                mScanDeviceList!!.forEachIndexed { _, scanResultEntity ->
                    if (scanResultEntity.device != null &&
                            Objects.equals(bluetoothDevice.address, scanResultEntity.device.address)) {
                        scanResultEntity.state = "已连接"
                    }
                }
            }
        }

        if (adapterShowResult == null) {
            adapterShowResult = ScanResultAdapter(mScanDeviceList!!)
            adapterShowResult!!.setListener(object : ScanResultAdapter.OnItemClickListener() {
                override fun onItemClick(device: BluetoothDevice, position: Int) {
                    val deviceClass = device.bluetoothClass
                    //如果是音频类型的设备才能进行连接
                    if (deviceClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                            deviceClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) {
                        println("click called   name=" + device.name + ",position=$position")
                        when (device.bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                if (mPresenter!!.isDiscovering()) {
                                    mPresenter!!.cancelDiscovery()
                                }
                                if (mPresenter!!.isConnected(device)) {
                                    mPresenter!!.disconnectDevice(device)
                                } else {
                                    mPresenter!!.connectDevice(device)
                                }
                                mPresenter!!.startDiscovery()
                            }
                            BluetoothDevice.BOND_BONDING -> {//正在配对
                                mPresenter!!.getExitsDevices()
                            }
                            BluetoothDevice.BOND_NONE -> {//未配对
                                mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_child, device))
                                adapterShowResult!!.updateBondedDevices(mScanDeviceList!!)
                                //开始配对
                                device.createBond()
                                mPresenter!!.getExitsDevices()
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
            adapterShowResult!!.updateBondedDevices(mScanDeviceList!!)
            adapterShowResult!!.notifyDataSetChanged()
        }
        mPresenter!!.startDiscovery()
    }

    private fun clearData() {
        if (adapterShowResult != null) {
            adapterShowResult!!.clearData()
        }
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
                mPresenter!!.getExitsDevices()
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
                mBlueToothOpenSwitch!!.isChecked = true
                checkLocationPermission()
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                mToolBar!!.title = "蓝牙正在关闭"
            }
            BluetoothAdapter.STATE_OFF -> {
                mToolBar!!.title = "蓝牙已关闭"
                mBlueToothOpenSwitch!!.isChecked = false
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
                println("停止扫描")
                stopDiscovery()
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                println("开始扫描")
                startDiscovery()

            }
        }
    }

    private fun startDiscovery() {
        if (mRefreshScanMenu == null) {
            mRefreshScanMenu = findViewById(R.id.action_refresh)
        }
        mLoadingAnim = RotateAnimation(0f, 360f, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mLoadingAnim!!.repeatCount = -1
        mLoadingAnim!!.interpolator = LinearInterpolator()
        mLoadingAnim!!.duration = 800
        if (mRefreshScanMenu != null) {
            mRefreshScanMenu!!.startAnimation(mLoadingAnim)
        }
    }

    private fun stopDiscovery() {
        if (mRefreshScanMenu != null) {
            mRefreshScanMenu!!.clearAnimation()
        }
        if (mLoadingAnim != null) {
            mLoadingAnim!!.reset()
            mLoadingAnim!!.cancel()
            mLoadingAnim = null
        }
    }

    fun onDeviceBondStateChanged(state: Int, device: BluetoothDevice) {
        when (state) {
            BluetoothDevice.BOND_BONDED -> {
                println("Device:" + device.name + " bonded.")
                mPresenter!!.getExitsDevices()
            }
            BluetoothDevice.BOND_BONDING -> {
                println("Device:" + device.name + " bonding.....")
            }
            BluetoothDevice.BOND_NONE -> {
                println("Device:" + device.name + " not bonded.")
                //不知道是蓝牙耳机的关系还是什么原因，经常配对不成功
                //配对不成功的话，重新尝试配对
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    device.createBond()
                }
            }
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
        //未知原因：连接上设备之后总是会最后调用一次断开连接，会导致状态显示错误，所以从连接设备列表中再次查询一次
        if (mPresenter!!.getBondDevice() != null && mPresenter!!.getBondDevice()!!.contains(device)) {
            val position = mPresenter!!.getBondDevice()!!.indexOf(device)
            val scanResultEntity = mScanDeviceList!![(1 + position)]
            scanResultEntity.state = stateStr
        }

        if(mPresenter!!.getConnectDevice() != null){
            mPresenter!!.getConnectDevice()!!.forEachIndexed { _, bluetoothDevice ->
                mScanDeviceList!!.forEachIndexed { _, scanResultEntity ->
                    if (scanResultEntity.device != null &&
                            Objects.equals(bluetoothDevice.address, scanResultEntity.device.address)) {
                        scanResultEntity.state = "已连接"
                    }
                }
            }
        }
        adapterShowResult!!.updateBondedDevices(mScanDeviceList!!)
    }

    override fun onResume() {
        super.onResume()
        if (mDeviceFoundReceiver == null) {
            mDeviceFoundReceiver = DeviceFoundReceiver(this)
        }
        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        foundFilter.addAction("android.bluetooth.device.action.DISAPPEARED")
        registerReceiver(mDeviceFoundReceiver, foundFilter)

        if (blStateChangeReceiver == null) {
            blStateChangeReceiver = StatusChangeReceiver(this)
        }

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
        adapterShowResult!!.foundNewDevice(ScanResultEntity(R.layout.item_scan_child, device))
    }

    override fun onPause() {
        super.onPause()
        if (mDeviceFoundReceiver != null) {
            unregisterReceiver(mDeviceFoundReceiver)
        }
        unregisterReceiver(blStateChangeReceiver)
        mPresenter!!.cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.REQUEST_ENABLE_BL && resultCode == Activity.RESULT_OK) {
            mPresenter!!.getExitsDevices()
            mPresenter!!.startDiscovery()
            onBLStateChange(BluetoothAdapter.STATE_ON)
        }
//        else if(requestCode != 1){
//            println("$requestCode...$resultCode....$data")
//            showToast("蓝牙开启失败")
//            onBLStateChange(BluetoothAdapter.STATE_OFF)
//        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun showToast(msg: String) {
        EasyTintView.makeText(recyclerShowResult!!, msg, EasyTintView.TINT_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter!!.destroyView()
    }
}

package com.zy.ppmusic.mvp.view

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.ScanResultCopyAdapter
import com.zy.ppmusic.adapter.base.OnItemViewClickListener
import com.zy.ppmusic.entity.ScanResultEntity
import com.zy.ppmusic.mvp.base.AbstractBaseMvpActivity
import com.zy.ppmusic.mvp.contract.IBLActivityContract
import com.zy.ppmusic.mvp.presenter.BlActivityPresenter
import com.zy.ppmusic.receiver.DeviceFoundReceiver
import com.zy.ppmusic.receiver.StatusChangeReceiver
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.UIUtils
import com.zy.ppmusic.widget.EasyTintView
import kotlinx.android.synthetic.main.activity_bl_scan.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

/**
 * @author ZY
 */
class BlScanActivity : AbstractBaseMvpActivity<BlActivityPresenter>(), IBLActivityContract.IBLActivityView, EasyPermissions.PermissionCallbacks {
    override fun getContentViewId(): Int = R.layout.activity_bl_scan

    override fun createPresenter(): BlActivityPresenter = BlActivityPresenter(this)

    private val blEnableRequestCode = 0x001
    private var mBlueToothOpenSwitch: SwitchCompat? = null//蓝牙开关
    private var mBlStateChangeReceiver: StatusChangeReceiver? = null
    private var mScanDeviceList: ArrayList<ScanResultEntity>? = null
    private var mScanResultRecycler: RecyclerView? = null
    private var mScanResultAdapter: ScanResultCopyAdapter? = null
    private var mDeviceFoundReceiver: DeviceFoundReceiver? = null
    private var mLoadingAnim: RotateAnimation? = null
    private var mToolBar: Toolbar? = null
    private var mRefreshScanMenu: View? = null

    override fun initViews() {
        mToolBar = findViewById(R.id.toolbar_bl)
        mScanResultRecycler = findViewById(R.id.show_device_recycler)
        if (mPresenter!!.isSupportBl().not()) {
            Toast.makeText(this, UIUtils.getString(R.string.unsupport_bluetooth), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mScanDeviceList = ArrayList()
        mBlueToothOpenSwitch = findViewById(R.id.switch_bl)
        mToolBar?.title = if (mPresenter!!.isEnable()) {
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
        mBlueToothOpenSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 打开蓝牙
                if (mPresenter!!.isEnable().not()) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    // 设置蓝牙可见性，最多300秒
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    startActivityForResult(intent, blEnableRequestCode)
                }
            } else {
                mPresenter!!.disable()
            }
        }

        root_bl_content.setDragFinishListener {
            finish()
        }
    }

    /**
     * 检查权限，6.0之后需要权限才能接收到新设备
     */
    private fun checkLocationPermission() {
        if (!EasyPermissions.hasPermissions(applicationContext, "android.permission.ACCESS_COARSE_LOCATION")) {
            println("没有权限")
            if (!EasyPermissions.permissionPermanentlyDenied(this, "android.permission.ACCESS_COARSE_LOCATION")) {
                EasyPermissions.requestPermissions(this, "获取粗略位置用来加快扫描",
                        1, "android.permission.ACCESS_COARSE_LOCATION")
            } else {
                val dialog = AppSettingsDialog.Builder(this)
                dialog.setRationale("没有位置信息将无法获取新设备，这是安卓6.0之后的系统要求，请允许权限")
                dialog.setNegativeButton("任性不给")
                dialog.setPositiveButton("赏给你")
                dialog.build().show()
                println("被关到小黑屋了")
            }
        } else {
            println("已经有权限了")
        }
    }

    /**
     * 权限申请回调
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * 权限申请通过
     */
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        println("权限通过了")
        if (requestCode == 1) {
            if (mPresenter!!.isEnable()) {
                mPresenter!!.startDiscovery()
            }
        }
    }

    /**
     * 权限申请不通过
     */
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        if (requestCode == 1) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms!!)) {
                println("权限新特性")
                val dialog = AppSettingsDialog.Builder(this)
                dialog.setRationale("没有位置信息将无法获取新设备，这是安卓6.0之后的系统要求，请允许权限")
                dialog.build().show()
            } else {
                println("重新申请")
                EasyPermissions.requestPermissions(this, "获取粗略位置用来加快扫描,否则无法发现新设备",
                        1, "android.permission.ACCESS_COARSE_LOCATION")
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    @SuppressLint("MissingPermission")
    override fun setExitsDevices(exitList: MutableList<ScanResultEntity>) {
        mScanDeviceList!!.clear()
        mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_title, "已配对的设备"))
        mScanDeviceList!!.addAll(exitList)
        mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_title, "扫描到的设备"))

        checkConnectDevice()

        if (mScanResultAdapter == null) {
            mScanResultAdapter = ScanResultCopyAdapter(mScanDeviceList!!)
            mScanResultAdapter!!.setItemClickListener(OnItemViewClickListener { _, position ->
                val device = mScanResultAdapter!!.getDevice(position)
                if (device != null) {
                    val state = device.bondState
                    if (state == BluetoothDevice.BOND_BONDED) {
                        var realPosition = 0
                        if (!mScanResultAdapter!!.isBondedDevice(position)) {
                        }
                        if (mPresenter!!.removeBondDevice(mScanDeviceList!![realPosition].device!!)) {
                            mScanDeviceList!!.removeAt(realPosition)
                            println("移除配对")
                            mScanResultAdapter!!.updateBondedDevices(mScanDeviceList!!)
                        }
                    }
                    PrintLog.d("点击Item")
                } else {
                    PrintLog.d("点击title了")
                }
            })
//            mScanResultAdapter!!.setListener(object : ScanResultAdapter.OnItemClickListener() {
//                override fun onItemOtherClick(view: View?, position: Int) {
//                    if (position >= 0 && position < mScanDeviceList!!.size) {
//                        val state = mScanDeviceList!![position].device.bondState
//                        if (state == BluetoothDevice.BOND_BONDED) {
//                            if (mPresenter!!.removeBondDevice(mScanDeviceList!![position].device)) {
//                                mScanDeviceList!!.removeAt(position)
//                                println("移除配对")
//                                mScanResultAdapter!!.updateBondedDevices(mScanDeviceList!!)
//                            }
//                        }
//                    }
//                }
//
//                override fun onItemClick(device: BluetoothDevice, position: Int) {
//                    val deviceClass = device.bluetoothClass
//                    //如果是音频类型的设备才能进行连接
//                    if (deviceClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
//                            deviceClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) {
//                        println("click called   name=" + device.name + ",position=$position")
//                        when (device.bondState) {
//                            BluetoothDevice.BOND_BONDED -> {
//                                if (mPresenter!!.isDiscovering()) {
//                                    mPresenter!!.cancelDiscovery()
//                                }
//                                if (mPresenter!!.isConnected(device)) {
//                                    mPresenter!!.disconnectDevice(device)
//                                } else {
//                                    mPresenter!!.connectDevice(device)
//                                }
//                                mPresenter!!.startDiscovery()
//                            }
//                            BluetoothDevice.BOND_BONDING -> {//正在配对
//                                mPresenter!!.getExitsDevices()
//                            }
//                            BluetoothDevice.BOND_NONE -> {//未配对
//                                mScanDeviceList!!.add(ScanResultEntity(R.layout.item_scan_child, device))
//                                mScanResultAdapter!!.updateBondedDevices(mScanDeviceList!!)
//                                //开始配对
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                                    device.createBond()
//                                }
//                                mPresenter!!.getExitsDevices()
//                            }
//                        }
//                    } else {
//                        showToast("此设备不可作为蓝牙耳机")
//                    }
//                }
//            })
            mScanResultRecycler!!.adapter = mScanResultAdapter
            mScanResultRecycler!!.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        } else {
            mScanResultAdapter!!.updateBondedDevices(mScanDeviceList!!)
            mScanResultAdapter!!.notifyDataSetChanged()
        }
        mPresenter!!.startDiscovery()
    }

    private fun clearData() {
        if (mScanResultAdapter != null) {
            mScanResultAdapter!!.clearData()
        }
    }

    override fun getContext(): Context = applicationContext

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

    /**
     * 扫描状态更新
     */
    fun discoveryStateChange(state: String) {
        when (state) {
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                println("停止扫描")
                stopDiscoveryAnim()
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                println("开始扫描")
                startDiscoveryAnim()
            }
        }
    }

    /**
     * 开始扫描动画
     */
    private fun startDiscoveryAnim() {
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

    /**
     * 停止扫描动画
     */
    private fun stopDiscoveryAnim() {
        if (mRefreshScanMenu != null) {
            mRefreshScanMenu!!.clearAnimation()
        }
        if (mLoadingAnim != null) {
            mLoadingAnim!!.reset()
            mLoadingAnim!!.cancel()
            mLoadingAnim = null
        }
    }

    @SuppressLint("MissingPermission")
            /**
             * 当设备配对状态变化时回调
             */
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

    /**
     * 连接状态变化时回调
     */
    fun connectStateChanged() {
        //未知原因：连接上设备之后总是会最后调用一次断开连接，会导致状态显示错误，所以遍历整个设备列表查询一次
        mScanDeviceList!!.forEachIndexed { _, entity ->
            if (entity.device != null) {
                val connectState = mPresenter!!.getConnectState(entity.device!!)
                when (connectState) {
                    BluetoothA2dp.STATE_CONNECTING -> {
                        entity.state = "正在连接"
                    }
                    BluetoothA2dp.STATE_DISCONNECTED -> {
                        entity.state = ""
                    }
                    BluetoothA2dp.STATE_CONNECTED -> {
                        entity.state = "已连接"
                    }
                }
            }
        }

        mScanResultAdapter!!.updateBondedDevices(mScanDeviceList!!)
    }

    override fun onResume() {
        super.onResume()
        if (mDeviceFoundReceiver == null) {
            mDeviceFoundReceiver = DeviceFoundReceiver(this)
        }
        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        foundFilter.addAction("android.bluetooth.device.action.DISAPPEARED")
        registerReceiver(mDeviceFoundReceiver, foundFilter)

        if (mBlStateChangeReceiver == null) {
            mBlStateChangeReceiver = StatusChangeReceiver(this)
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        registerReceiver(mBlStateChangeReceiver, filter)
        checkConnectDevice()
    }

    private fun checkConnectDevice() {
        if (mPresenter!!.getConnectDevice() != null) {
            mScanDeviceList!!.forEachIndexed { _, scanResultEntity ->
                if (scanResultEntity.device != null) {
                    val state = mPresenter!!.getConnectState(scanResultEntity.device!!)
                    when (state) {
                        BluetoothA2dp.STATE_CONNECTING -> {
                            scanResultEntity.state = "正在连接"
                        }
                        BluetoothA2dp.STATE_DISCONNECTED -> {
                            scanResultEntity.state = ""
                        }
                        BluetoothA2dp.STATE_CONNECTED -> {
                            scanResultEntity.state = "已连接"
                        }
                    }
                }
            }
        }
        if (mScanResultAdapter != null) {
            mScanResultAdapter!!.updateBondedDevices(mScanDeviceList!!)
        }
    }

    /**
     *  找到新设备添加到列表中
     */
    fun foundNewDevice(device: BluetoothDevice) {
        println("新发现的设备。。。" + device.toString())
        mScanResultAdapter!!.foundNewDevice(ScanResultEntity(R.layout.item_scan_child, device))
    }

    override fun onPause() {
        super.onPause()
        if (mDeviceFoundReceiver != null) {
            unregisterReceiver(mDeviceFoundReceiver)
        }
        unregisterReceiver(mBlStateChangeReceiver)
        mPresenter!!.cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.blEnableRequestCode && resultCode == Activity.RESULT_OK) {
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

    fun showToast(msg: String) = EasyTintView.makeText(mScanResultRecycler!!, msg, EasyTintView.TINT_SHORT).show()

}

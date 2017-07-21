package com.zy.ppmusic.bl

import android.app.Activity
import android.bluetooth.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.ScanResultAdapter
import com.zy.ppmusic.entity.ScanResultEntity
import com.zy.ppmusic.receiver.DeviceFoundReceiver
import com.zy.ppmusic.receiver.KeyDownReceiver
import com.zy.ppmusic.receiver.StatusChangeReceiver
import java.util.*

class BlScanActivity : AppCompatActivity() {
    private val TAG = "BlScanActivity"
    private val REQUEST_ENABLE_BL = 0x001

    private var tv_bl_status: TextView? = null
    private var sw_bl: SwitchCompat? = null
    private var defaultAdapter: BluetoothAdapter? = null
    private var blStateChangeReceiver: StatusChangeReceiver? = null
    private var scanList: ArrayList<ScanResultEntity>? = null
    private var recyclerShowResult: RecyclerView? = null
    private var adapterShowResult: ScanResultAdapter? = null
    private var context: Context? = null
    private var blDeviceFoundReceiver: DeviceFoundReceiver? = null
    private var receiverManager:LocalBroadcastManager ?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bl_scan)
        val toolbar = findViewById(R.id.toolbar_bl) as Toolbar
        setSupportActionBar(toolbar)

        defaultAdapter = BluetoothAdapter.getDefaultAdapter()

        if (defaultAdapter == null) {
            showToast("设备不支持蓝牙")
            finish()
            return
        }
        context = this
        scanList = ArrayList()
        recyclerShowResult = findViewById(R.id.show_device_recycler) as RecyclerView
        tv_bl_status = findViewById(R.id.tv_bl_status) as TextView
        sw_bl = findViewById(R.id.switch_bl) as SwitchCompat
        tv_bl_status!!.text = if (defaultAdapter!!.isEnabled) {
            sw_bl!!.isChecked = true
            updateExitDevice()
            defaultAdapter!!.startDiscovery()
            "蓝牙已开启"
        } else {
            sw_bl!!.isChecked = false
            "蓝牙已关闭"
        }

        receiverManager = LocalBroadcastManager.getInstance(this)

        sw_bl!!.setOnCheckedChangeListener({ _, isChecked ->
            if (isChecked) {
                // 打开蓝牙
                if (!defaultAdapter!!.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    // 设置蓝牙可见性，最多300秒
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    startActivityForResult(intent, REQUEST_ENABLE_BL)
                }
            } else {
                defaultAdapter!!.disable()
            }
        })
    }

    /**
     * 连接设备
     */
    fun connectDevice(device: BluetoothDevice) {
        defaultAdapter!!.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (proxy is BluetoothHeadset) {
                    try {
                        val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        method.isAccessible = true
                        method.invoke(proxy, device)
                        println("connect device success name="+device.name)
                        if(defaultAdapter!!.isDiscovering){
                            defaultAdapter!!.cancelDiscovery()
                        }
                        showToast("连接成功 "+device.name)
                        open(context!!,true)
                        refreshDevice()
                        defaultAdapter!!.startDiscovery()
                    } catch (e: Exception) {
                        println("connect device error..."+e.message)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }, BluetoothProfile.HEADSET)
    }


    /**
     * 刷新设备列表
     */
    fun refreshDevice() {
        updateExitDevice()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bl_refresh, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_refresh -> {
                if (defaultAdapter != null) {
                    refreshDevice()
                    defaultAdapter!!.cancelDiscovery()
                    defaultAdapter!!.startDiscovery()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 添加已配对的设备列表
     */
    fun updateExitDevice() {
        val devices = defaultAdapter!!.bondedDevices
        scanList!!.clear()
        if (devices.size > 0) {
            scanList!!.add(ScanResultEntity(R.layout.item_scan_title, "已配对的设备"))
        }
        devices.forEachIndexed { index, bluetoothDevice ->
            println("$index,name=" + bluetoothDevice.name)
            scanList!!.add(ScanResultEntity(R.layout.item_scan_child, bluetoothDevice))
        }
        scanList!!.add(ScanResultEntity(R.layout.item_scan_title, "可用设备"))
        if (adapterShowResult == null) {
            adapterShowResult = ScanResultAdapter(scanList!!)
            adapterShowResult!!.setListener(object : ScanResultAdapter.OnItemClickListener() {
                override fun onItemClick(device: BluetoothDevice, position: Int) {
                    println("click called   name="+device.name+",position=$position")
                    if(!device.createBond()){//如果返回值为false，说明设备已经配对过了直接连接设备
                        connectDevice(device)
                    }
                }
            })
            recyclerShowResult!!.adapter = adapterShowResult
            recyclerShowResult!!.layoutManager = LinearLayoutManager(this)
        } else {
            adapterShowResult!!.updateData(scanList!!)
            adapterShowResult!!.notifyDataSetChanged()
        }
    }

    /**
     * 为MEDIA_BUTTON 意图注册接收器（注册开启耳机线控监听, 请务必在设置接口监听之后再调用此方法，否则接口无效）
     * @param context
     */
    @RequiresApi(Build.VERSION_CODES.FROYO)
    fun open(context:Context,isRegister:Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val name = ComponentName(context.packageName,KeyDownReceiver::class.java.name)
        if(isRegister)audioManager.registerMediaButtonEventReceiver(name)
        else audioManager.unregisterMediaButtonEventReceiver(name)
    }


    /**
     * 修改文字等其他信息
     */
    fun onBLStateChange(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_TURNING_ON -> {
                tv_bl_status!!.text = "蓝牙正在开启"
            }
            BluetoothAdapter.STATE_ON -> {
                tv_bl_status!!.text = "蓝牙已开启"
                sw_bl!!.isChecked = true
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                tv_bl_status!!.text = "蓝牙正在关闭"
            }
            BluetoothAdapter.STATE_OFF -> {
                tv_bl_status!!.text = "蓝牙已关闭"
                sw_bl!!.isChecked = false
            }
            else -> {
                println("收到其他状态...$state")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (blStateChangeReceiver == null) {
            blStateChangeReceiver = StatusChangeReceiver(this)
        }
        if (blDeviceFoundReceiver == null) {
            blDeviceFoundReceiver = DeviceFoundReceiver(this)
        }

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)

        receiverManager!!.registerReceiver(blDeviceFoundReceiver,foundFilter)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        receiverManager!!.registerReceiver(blStateChangeReceiver, filter)
    }

    /**
     *  找到新设备添加到列表中
     */
    fun foundNewDevice(device: BluetoothDevice) {
        println("新发现的设备。。。" + device.toString())
        if (!isInList(device)) {
            scanList!!.add(ScanResultEntity(R.layout.item_scan_child, device))
            println("此设备不存在列表。。。" + scanList!!.size)
            adapterShowResult!!.updateData(scanList!!)
            adapterShowResult!!.notifyDataSetChanged()
        } else println("已经存在了设备")
    }

    /**
     * 判断新发现的设备是否已经存在列表中
     */
    fun isInList(device: BluetoothDevice): Boolean {
        scanList!!.forEachIndexed { _, scanEntity ->
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
        scanList!!.forEachIndexed { _, scanResultEntity ->
            if (scanResultEntity.device == device) {
                scanList!!.remove(scanResultEntity)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode){
            KeyEvent.KEYCODE_MEDIA_NEXT->{
                println("下一首")
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE->{
                println("暂停")
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE->{
                println("播放或者暂停")
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS->{
                println("上一首")
            }
            KeyEvent.KEYCODE_VOLUME_DOWN->{
                println("音量减")
            }
            KeyEvent.KEYCODE_VOLUME_UP->{
                println("音量加")
            }else ->{
                return super.onKeyDown(keyCode, event)
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        receiverManager!!.unregisterReceiver(blDeviceFoundReceiver)
        receiverManager!!.unregisterReceiver(blStateChangeReceiver)
        defaultAdapter!!.cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.REQUEST_ENABLE_BL && resultCode == Activity.RESULT_OK) {
            showToast("蓝牙开启成功")
            updateExitDevice()
            defaultAdapter!!.startDiscovery()
            onBLStateChange(BluetoothAdapter.STATE_ON)
        } else {
            showToast("蓝牙开启失败")
            onBLStateChange(BluetoothAdapter.STATE_OFF)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (defaultAdapter != null && defaultAdapter!!.isDiscovering) {
            defaultAdapter!!.cancelDiscovery()
        }
        if(context != null){
            open(context!!,false)
        }
    }
}

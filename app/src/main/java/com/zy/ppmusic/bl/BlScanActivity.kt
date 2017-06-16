package com.zy.ppmusic.bl

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.zy.ppmusic.R

class BlScanActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BL = 0x001

    private var tv_bl_status: TextView? = null
    private var sw_bl: SwitchCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bl_scan)
        val toolbar = findViewById(R.id.toolbar_bl) as Toolbar
        setSupportActionBar(toolbar)


        val defaultAdapter: BluetoothAdapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            defaultAdapter = bluetoothManager.adapter
        }else{
            defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if(defaultAdapter == null){
            showToast("设备不支持蓝牙")
            finish()
            return;
        }

        tv_bl_status = findViewById(R.id.tv_bl_status) as TextView
        sw_bl = findViewById(R.id.switch_bl) as SwitchCompat
        tv_bl_status!!.text = if(defaultAdapter.isEnabled){
            sw_bl!!.isChecked = true
            "蓝牙已开启"
        }else{
            sw_bl!!.isChecked = false
            "蓝牙已关闭"
        }

        sw_bl!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            tv_bl_status!!.text = if(isChecked){
                // 打开蓝牙
                if (!defaultAdapter.isEnabled()) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    // 设置蓝牙可见性，最多300秒
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    startActivityForResult(intent, REQUEST_ENABLE_BL)
                }
                val devices = defaultAdapter.bondedDevices
                for (i in devices.indices) {
                    println(devices.iterator().next().name)
                }
                "蓝牙已打开"
            }else{
                defaultAdapter.disable()
                "蓝牙已关闭"
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == this.REQUEST_ENABLE_BL && resultCode == Activity.RESULT_OK){
            showToast("蓝牙开启成功")
            tv_bl_status!!.text = "蓝牙已开启"
            sw_bl!!.isChecked = true
        }else{
            showToast("蓝牙开启失败")
            sw_bl!!.isChecked = false
            tv_bl_status!!.text = "蓝牙已关闭"
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    fun showToast(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }
}

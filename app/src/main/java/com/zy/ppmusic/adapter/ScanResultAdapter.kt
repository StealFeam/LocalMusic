package com.zy.ppmusic.adapter

import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.entity.ScanResultEntity
import java.util.*
import kotlin.collections.ArrayList

class ScanResultAdapter(mData: ArrayList<ScanResultEntity>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mBondDevices: ArrayList<ScanResultEntity> = ArrayList()
    private val mScanDevices: ArrayList<ScanResultEntity> = ArrayList()
    private var listener: OnItemClickListener? = null

    init {
        this.mBondDevices.addAll(mData)
    }

    fun setListener(l: OnItemClickListener) {
        this.listener = l
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < mBondDevices.size) {
            mBondDevices[position].type
        } else {
            mScanDevices[position - mBondDevices.size].type
        }
    }

    fun clearData() {
        this.mBondDevices.clear()
        notifyDataSetChanged()
    }

    fun foundNewDevice(entity: ScanResultEntity) {
        if (isInList(entity.device)) {
            println("该设备已经存在列表了")
            return
        }
        mScanDevices.add(entity)
        notifyItemInserted(mBondDevices.size + mScanDevices.size - 1)
    }

    /**
     * 判断新发现的设备是否已经存在列表中
     */
    private fun isInList(device: BluetoothDevice): Boolean {
        mScanDevices.forEachIndexed { _, scanEntity ->
            if (scanEntity.device == device) {
                return true
            }
        }
        mBondDevices.forEachIndexed { _, scanResultEntity ->
            if(scanResultEntity.device == device){
                return true
            }
        }
        return false
    }

    fun deviceDisappeared(device: BluetoothDevice) {
        mScanDevices.forEachIndexed { index, scanResultEntity ->
            if (Objects.equals(scanResultEntity.device.address, device.address)) {
                mScanDevices.removeAt(index)
                return
            }
        }
    }

    fun updateBondedDevices(mData: ArrayList<ScanResultEntity>) {
        this.mBondDevices.clear()
        this.mScanDevices.clear()
        this.mBondDevices.addAll(mData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        when (viewType) {
            R.layout.item_scan_title -> {
                return TitleHolder(LayoutInflater.from(parent!!.context).inflate(R.layout.item_scan_title, parent, false))
            }
            R.layout.item_scan_child -> {
                return ScanResultHolder(LayoutInflater.from(parent!!.context).
                        inflate(R.layout.item_scan_child, parent, false), listener!!)
            }
        }
        return null
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder?, p1: Int) {
        if (p0 is TitleHolder) {
            p0.title!!.text = mBondDevices[p1].title
        } else if (p0 is ScanResultHolder) {
            val entity:ScanResultEntity? = if(p1 < mBondDevices.size){
                mBondDevices[p1]
            }else{
                mScanDevices[p1 - mBondDevices.size]
            }
            p0.name!!.text = entity!!.device.name
            p0.name!!.tag = entity.device
            println("position=" + p1 + "," + entity.state)
            if (!TextUtils.isEmpty(entity.state)) {
                p0.showState(entity.state)
            } else {
                p0.hideState()
            }
        }
    }

    private fun isTitlePosition(position: Int): Boolean {
        if (position == 0 || position == mBondDevices.size - 1) {
            return true
        }
        return false
    }

    override fun getItemCount(): Int {
        return mBondDevices.size + mScanDevices.size
    }

    class TitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null

        init {
            title = itemView.findViewById(R.id.tv_scan_result_title) as TextView
        }
    }

    class ScanResultHolder(itemView: View, l: OnItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(v: View?) {
            if (listener != null) {
                listener!!.onItemClick(name!!.tag as BluetoothDevice, adapterPosition)
            }
        }

        var name: TextView? = null
        var tvState: TextView? = null
        var icon: ImageView? = null
        var listener: OnItemClickListener? = null

        init {
            this.listener = l
            itemView.setOnClickListener(this)
            icon = itemView.findViewById(R.id.iv_scan_result_icon) as ImageView
            name = itemView.findViewById(R.id.tv_scan_result_name) as TextView
            tvState = itemView.findViewById(R.id.tv_connect_state) as TextView
        }

        fun showState(state: String) {
            tvState!!.text = state
            if (tvState!!.visibility != View.VISIBLE) {
                tvState!!.visibility = View.VISIBLE
            }
        }

        fun hideState() {
            if (tvState!!.visibility == View.VISIBLE) {
                tvState!!.visibility = View.GONE
            }
        }

    }

    abstract class OnItemClickListener {
        abstract fun onItemClick(device: BluetoothDevice, position: Int)
    }

}
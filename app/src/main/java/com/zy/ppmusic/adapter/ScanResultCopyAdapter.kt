package com.zy.ppmusic.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.base.AbstractMultipleTypeAdapter
import com.zy.ppmusic.adapter.base.ExpandableViewHolder
import com.zy.ppmusic.adapter.base.OnItemViewClickListener
import com.zy.ppmusic.entity.ScanResultEntity

/**
 * @author stealfeam
 * @date 2018/2/9
 */
class ScanResultCopyAdapter(mData: ArrayList<ScanResultEntity>) : AbstractMultipleTypeAdapter() {
    private var mBondDevices: ArrayList<ScanResultEntity> = ArrayList()
    private val mScanDevices: ArrayList<ScanResultEntity> = ArrayList()
    private var listener: ScanResultAdapter.OnItemClickListener? = null
    private var mItemClickListener: OnItemViewClickListener? = null

    init {
        this.mBondDevices.addAll(mData)
    }

    fun setListener(l: ScanResultAdapter.OnItemClickListener) {
        this.listener = l
    }

    fun setItemClickListener(itemClickListener: OnItemViewClickListener) {
        this.mItemClickListener = itemClickListener
    }

    fun clearData() {
        this.mBondDevices.clear()
        this.mScanDevices.clear()
        notifyDataSetChanged()
    }

    fun foundNewDevice(entity: ScanResultEntity) {
        if (isInList(entity.device!!)) {
            println("该设备已经存在列表了")
            return
        }
        mScanDevices.add(entity)
        notifyItemInserted(mBondDevices.size + mScanDevices.size - 1)
    }

    private fun getBondedDevice(position: Int):BluetoothDevice?{
        return if(position > 0 && position < (mBondDevices.size + 2)){
            mBondDevices[position-1].device
        }else null
    }

    fun isBondedDevice(position: Int):Boolean = position>0 && position<(mBondDevices.size + 2)

    fun getDevice(position: Int):BluetoothDevice?{
        if (isBondedDevice(position)){
            return mBondDevices[position].device
        }
        val bondedDevice = getBondedDevice(position)
        if(bondedDevice != null){
            return bondedDevice
        }
        val realPosition = position - 2 - mBondDevices.size
        return mScanDevices[realPosition].device
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
            if (scanResultEntity.device == device) {
                return true
            }
        }
        return false
    }

    fun updateBondedDevices(mData: ArrayList<ScanResultEntity>) {
        this.mBondDevices.clear()
        this.mScanDevices.clear()
        this.mBondDevices.addAll(mData)
        notifyDataSetChanged()
    }

    override fun getItemTypeByPosition(position: Int): Int {
        return if (position < mBondDevices.size) {
            mBondDevices[position].type
        } else {
            mScanDevices[position - mBondDevices.size].type
        }
    }

    override fun getItemLayoutIdByType(viewType: Int): Int = viewType

    override fun bindHolderData(holder: ExpandableViewHolder?, viewType: Int) {
        when (viewType) {
            R.layout.item_scan_child -> {
                holder?.attachOnClickListener(mItemClickListener, holder.itemView)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun setupItemData(holder: ExpandableViewHolder?, position: Int) {
        when (getItemTypeByPosition(position)) {
            R.layout.item_scan_title -> {
                val titleTv: TextView = holder!!.getView(R.id.tv_scan_result_title)
                titleTv.text = mBondDevices[position].title
            }
            R.layout.item_scan_child -> {
                val entity: ScanResultEntity? = if (position < mBondDevices.size) {
                    mBondDevices[position]
                } else {
                    mScanDevices[position - mBondDevices.size]
                }
                val nameTv = holder!!.getView<TextView>(R.id.tv_scan_result_name)
                nameTv.text = if (entity!!.device?.name == null) {
                    "unknown"
                } else {
                    entity.device?.name
                }
                nameTv.tag = entity.device
                println("position=" + position + "," + entity.state)
                val delBondIv = holder.getView<ImageView>(R.id.bl_del_bond_iv)
                if (mBondDevices.contains(entity)) {
                    val tvState = holder.getView<TextView>(R.id.tv_connect_state)
                    if (!TextUtils.isEmpty(entity.state)) {
                        tvState!!.text = entity.state
                        if (tvState.visibility != View.VISIBLE) {
                            tvState.visibility = View.VISIBLE
                        }
                    } else {
                        if (tvState.visibility == View.VISIBLE) {
                            tvState.visibility = View.GONE
                        }
                    }
                    delBondIv.visibility = View.VISIBLE
                } else {
                    delBondIv.visibility = View.GONE
                }
            }
        }
    }

    override fun itemCount(): Int = mBondDevices.size + mScanDevices.size

}
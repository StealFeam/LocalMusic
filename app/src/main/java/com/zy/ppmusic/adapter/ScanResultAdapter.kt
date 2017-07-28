package com.zy.ppmusic.adapter

import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.entity.ScanResultEntity
import android.text.TextUtils


class ScanResultAdapter(mData:ArrayList<ScanResultEntity>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mData:ArrayList<ScanResultEntity>? = null
    private var listener:OnItemClickListener ?= null
    init {
        this.mData = mData;
    }

    fun setListener(l:OnItemClickListener){
        this.listener = l
    }

    override fun getItemViewType(position: Int): Int {
        return mData!![position].type
    }

    fun clearData(){
        if(this.mData != null){
            this.mData!!.clear()
            notifyDataSetChanged()
        }
    }

    fun updateData(mData: ArrayList<ScanResultEntity>){
        this.mData = mData
    }

    fun updateChildData(position: Int){
        notifyItemChanged((position + 1))
    }

    override fun onCreateViewHolder(p0: ViewGroup?, p1: Int): RecyclerView.ViewHolder {
        when(p1){
            R.layout.item_scan_title->{
                return TitleHolder(LayoutInflater.from(p0!!.context).inflate(R.layout.item_scan_title,p0,false))
            }
            R.layout.item_scan_child->{
                return ScanResultHolder(LayoutInflater.from(p0!!.context).
                        inflate(R.layout.item_scan_child,p0,false),listener!!)
            }
        }
        TODO()
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder?, p1: Int) {
        if(p0 is TitleHolder){
            p0.title!!.text = mData!![p1].title
        }else if(p0 is ScanResultHolder){
            p0.name!!.text = mData!![p1].device.name
            p0.name!!.tag = mData!![p1].device
            println("position="+p1+","+mData!![p1].state)
            if(!TextUtils.isEmpty(mData!![p1].state)){
                p0.showState(mData!![p1].state)
            }else{
                p0.hideState()
            }
        }
    }

    override fun getItemCount(): Int {
        return if(mData == null){
            0
        }else{
            mData!!.size
        }
    }

    class TitleHolder(itemView:View) : RecyclerView.ViewHolder(itemView){
        var title:TextView?=null
        init {
            title = itemView.findViewById(R.id.tv_scan_result_title) as TextView
        }
    }

    class ScanResultHolder(itemView:View,l:OnItemClickListener): RecyclerView.ViewHolder(itemView),View.OnClickListener {
        override fun onClick(v: View?) {
            if(listener != null){
                listener!!.onItemClick(name!!.tag as BluetoothDevice,adapterPosition)
            }
        }

        var name:TextView?=null
        var tvState:TextView?=null
        var icon:ImageView?=null
        var listener:OnItemClickListener ?= null
        init {
            this.listener = l
            itemView.setOnClickListener(this)
            icon = itemView.findViewById(R.id.iv_scan_result_icon) as ImageView
            name = itemView.findViewById(R.id.tv_scan_result_name) as TextView
            tvState = itemView.findViewById(R.id.tv_connect_state) as TextView
        }

        fun showState(state:String){
            tvState!!.text  = state
            if(tvState!!.visibility != View.VISIBLE){
                tvState!!.visibility = View.VISIBLE
            }
        }

        fun hideState(){
            if(tvState!!.visibility == View.VISIBLE){
                tvState!!.visibility = View.GONE
            }
        }

    }

    abstract class OnItemClickListener{
        abstract fun onItemClick(device:BluetoothDevice,position: Int)
    }

}
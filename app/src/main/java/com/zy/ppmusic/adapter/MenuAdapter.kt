package com.zy.ppmusic.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import com.zy.ppmusic.R
import com.zy.ppmusic.entity.MenuEntity

import java.util.ArrayList

/**
 * @author ZhiTouPC
 */
class MenuAdapter(context: Context) : BaseAdapter() {

    private var menuItemList: MutableList<MenuEntity>? = null

    init {
        buildMenuList(context)
    }

    private fun buildMenuList(context: Context) {
        menuItemList = ArrayList()
        menuItemList!!.add(MenuEntity(R.drawable.ic_loop_model_normal,
                context.getString(R.string.string_loop_model_list)))
        menuItemList!!.add(MenuEntity(R.drawable.ic_loop_model_only,
                context.getString(R.string.string_loop_model_only)))
        menuItemList!!.add(MenuEntity(R.drawable.ic_loop_mode_list,
                context.getString(R.string.string_loop_model_list_recycle)))
    }

    override fun getCount(): Int {
        return if (menuItemList == null) 0 else menuItemList!!.size
    }

    override fun getItem(position: Int): Any {
        return menuItemList!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }



    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder:MenuHolder
        var itemView = convertView

        if (itemView == null) {
            itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_menu, parent, false)
            holder = MenuHolder()
            holder.menuIcon = itemView!!.findViewById(R.id.item_menu_icon)
            holder.menuTitle = itemView.findViewById(R.id.item_menu_title)
            itemView.tag = holder
        } else {
            holder = itemView.tag as MenuHolder
        }
        holder.menuIcon!!.setImageResource(menuItemList!![position].icon)
        holder.menuTitle!!.text = menuItemList!![position].title.toString()
        return itemView
    }


    private class MenuHolder {
        var menuIcon: ImageView? = null
        var menuTitle: TextView? = null
    }
}

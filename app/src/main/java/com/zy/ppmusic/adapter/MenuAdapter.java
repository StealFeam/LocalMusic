package com.zy.ppmusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.entity.MenuEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * @author ZhiTouPC
 */
public class MenuAdapter extends BaseAdapter {
    private List<MenuEntity> menuItemList;

    public MenuAdapter(Context context) {
        buildMenuList(context);
    }

    private void buildMenuList(Context context) {
        menuItemList = new ArrayList<>();
        menuItemList.add(new MenuEntity(R.drawable.ic_loop_model_normal,
                context.getString(R.string.string_loop_model_list)));
        menuItemList.add(new MenuEntity(R.drawable.ic_loop_model_only,
                context.getString(R.string.string_loop_model_only)));
        menuItemList.add(new MenuEntity(R.drawable.ic_loop_mode_list,
                context.getString(R.string.string_loop_model_list_recycle)));
    }

    @Override
    public int getCount() {
        return menuItemList == null ? 0 : menuItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return menuItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu,parent,false);
            holder = new MenuHolder();
            holder.menuIcon = (ImageView) convertView.findViewById(R.id.item_menu_icon);
            holder.menuTitle = (TextView) convertView.findViewById(R.id.item_menu_title);
            convertView.setTag(holder);
        }else{
            holder = (MenuHolder) convertView.getTag();
        }
        holder.menuIcon.setImageResource(menuItemList.get(position).getIcon());
        holder.menuTitle.setText(String.valueOf(menuItemList.get(position).getTitle()));

        return convertView;
    }


    private static class MenuHolder {
        private ImageView menuIcon;
        private TextView menuTitle;
    }
}

package com.zy.ppmusic.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.entity.MainMenuEntity;

import java.util.List;

public class MainMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<MainMenuEntity> mDatas;

    public MainMenuAdapter(List<MainMenuEntity> mDatas) {
        this.mDatas = mDatas;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new MenuHolder(LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.item_main_menu,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        MenuHolder menuHolder = (MenuHolder) viewHolder;
        menuHolder.menuTitle.setText(mDatas.get(i).getMenuTitle());
        menuHolder.menuIcon.setImageResource(mDatas.get(i).getMenuRes());
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    private static class MenuHolder extends RecyclerView.ViewHolder {
        private ImageView menuIcon;
        private TextView menuTitle;

        private MenuHolder(View itemView) {
            super(itemView);
            menuIcon = (ImageView) itemView.findViewById(R.id.main_menu_icon);
            menuTitle = (TextView) itemView.findViewById(R.id.main_menu_title);
        }
    }
}

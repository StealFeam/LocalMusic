package com.zy.ppmusic.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.entity.MainMenuEntity;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MainMenuAdapter";
    private List<MainMenuEntity> mDatas;
    private OnRecycleItemClickListener listener;

    public MainMenuAdapter(List<MainMenuEntity> mDatas) {
        this.mDatas = mDatas;
    }

    public void setListener(OnRecycleItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new MenuHolder(LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.item_main_menu, viewGroup, false),listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        MenuHolder menuHolder = (MenuHolder) viewHolder;

        String s = ".../fffafsdf.0;";
        String ss = s.substring(s.lastIndexOf("/"),s.lastIndexOf("."));
        menuHolder.menuTitle.setText(mDatas.get(i).getMenuTitle());
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    private static class MenuHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
//        private ImageView menuIcon;
        private TextView menuTitle;
        private WeakReference<OnRecycleItemClickListener> weakReference;

        private MenuHolder(View itemView,OnRecycleItemClickListener l) {
            super(itemView);
            if(l != null){
                weakReference = new WeakReference<OnRecycleItemClickListener>(l);
            }
//            menuIcon = (ImageView) itemView.findViewById(R.id.main_menu_icon);
            menuTitle = (TextView) itemView.findViewById(R.id.main_menu_title);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(weakReference != null){
                weakReference.get().onItemClick(itemView,getAdapterPosition());
            }
        }
    }

    public interface OnRecycleItemClickListener {
        void onItemClick(View view, int position);
    }
}

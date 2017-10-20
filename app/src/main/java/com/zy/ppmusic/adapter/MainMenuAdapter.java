package com.zy.ppmusic.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
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
/**
 * @author ZY
 */
public class MainMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MainMenuAdapter";
    private List<MainMenuEntity> mDatas;
    private OnRecycleItemClickListener listener;
    private Context mContext;

    public MainMenuAdapter(List<MainMenuEntity> mDatas) {
        this.mDatas = mDatas;
    }

    public void setListener(OnRecycleItemClickListener listener) {
        this.listener = listener;
    }

    public void onUpdateItemTitle(int position,String title){
        MainMenuEntity itemByTitle = getItemByPosition(position);
        if(itemByTitle != null){
            itemByTitle.setMenuTitle(title);
            notifyItemChanged(position);
        }
    }

    private MainMenuEntity getItemByPosition(int pos){
        return mDatas.get(pos);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();
        return new MenuHolder(LayoutInflater.from(mContext).inflate(
                R.layout.item_main_menu, viewGroup, false),listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        MenuHolder menuHolder = (MenuHolder) viewHolder;
        Drawable topDrawable = ContextCompat.getDrawable(mContext, mDatas.get(i).getMenuRes());
        menuHolder.menuTitle.setCompoundDrawablesWithIntrinsicBounds(null,topDrawable,null,null);
        menuHolder.menuTitle.setText(mDatas.get(i).getMenuTitle());
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    private static class MenuHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView menuTitle;
        private WeakReference<OnRecycleItemClickListener> weakReference;

        private MenuHolder(View itemView,OnRecycleItemClickListener l) {
            super(itemView);
            if(l != null){
                weakReference = new WeakReference<>(l);
            }
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
        /**
         * item点击回调
         * @param view itemView
         * @param position itemPosition
         */
        void onItemClick(View view, int position);
    }
}

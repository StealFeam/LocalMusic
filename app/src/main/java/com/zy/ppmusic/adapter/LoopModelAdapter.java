package com.zy.ppmusic.adapter;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zy.ppmusic.R;

import java.util.ArrayList;
import java.util.List;

public class LoopModelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<LoopModelEntity> entityList;
    private MainMenuAdapter.OnRecycleItemClickListener listener;

    public LoopModelAdapter() {
        createList();
    }

    public void setListener(MainMenuAdapter.OnRecycleItemClickListener listener) {
        this.listener = listener;
    }

    public LoopModelEntity getItem(int pos) {
        if (entityList != null && pos >= 0 && pos < entityList.size()) {
            for (LoopModelEntity loopModelEntity : entityList) {
                loopModelEntity.isSelected = false;
            }
            return entityList.get(pos);
        }
        return null;
    }

    public void changeItem() {
        notifyDataSetChanged();
    }

    private void createList() {
        entityList = new ArrayList<>();
        entityList.add(new LoopModelEntity(R.drawable.ic_loop_model_normal, "列表播放", true));
        entityList.add(new LoopModelEntity(R.drawable.ic_loop_model_only, "单曲循环", false));
        entityList.add(new LoopModelEntity(R.drawable.ic_loop_mode_list, "列表循环", false));
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LoopModelHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_loop_model, parent, false), listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LoopModelHolder loopModelHolder = (LoopModelHolder) holder;
        loopModelHolder.iconIvBg.setBackgroundResource(entityList.get(position).icon);
        loopModelHolder.contentTv.setText(entityList.get(position).content);
        if (entityList.get(position).isSelected) {
            loopModelHolder.itemView.setBackgroundColor(ContextCompat.getColor(
                    loopModelHolder.itemView.getContext(), R.color.colorTheme));
            loopModelHolder.itemView.getBackground().setAlpha(100);
        } else {
            loopModelHolder.itemView.setBackgroundColor(ContextCompat.getColor(
                    loopModelHolder.itemView.getContext(), R.color.colorWhite));
        }
    }

    @Override
    public int getItemCount() {
        if (entityList != null) {
            return entityList.size();
        }
        return 0;
    }

    private class LoopModelHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView contentTv;
        private ImageView iconIvBg;
        private MainMenuAdapter.OnRecycleItemClickListener listener;

        private LoopModelHolder(View itemView, MainMenuAdapter.OnRecycleItemClickListener listener) {
            super(itemView);
            this.listener = listener;
            contentTv = (TextView) itemView.findViewById(R.id.item_icon_model_content);
            iconIvBg = (ImageView) itemView.findViewById(R.id.item_icon_model_desc);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    public class LoopModelEntity {
        public int icon;
        public String content;
        public boolean isSelected = false;

        private LoopModelEntity(int icon, String content, boolean isSelected) {
            this.icon = icon;
            this.content = content;
            this.isSelected = isSelected;
        }
    }
}

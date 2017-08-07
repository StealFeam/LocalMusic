package com.zy.ppmusic.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zy.ppmusic.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimeClockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private List<Integer> array;
    private MainMenuAdapter.OnRecycleItemClickListener listener;

    public TimeClockAdapter() {
        this.array = new ArrayList<>();
        for(int i=0;i<5;i++){
            this.array.add((i+1) * 15 );
        }
        this.array.add(120);
    }


    public int getItem(int position){
        return array.get(position);
    }

    public void setListener(MainMenuAdapter.OnRecycleItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TimeClockHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_list_normal,parent,false), listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TimeClockHolder clockHolder = (TimeClockHolder) holder;
        clockHolder.tvTime.setText(String.format(Locale.CHINA,"%d分钟",array.get(position)));
    }

    @Override
    public int getItemCount() {
        if(array == null){
            return 0;
        }
        return array.size();
    }

    private static class TimeClockHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView tvTime;
        private MainMenuAdapter.OnRecycleItemClickListener listener;
        private TimeClockHolder(View itemView, MainMenuAdapter.OnRecycleItemClickListener listener) {
            super(itemView);
            tvTime = (TextView) itemView.findViewById(R.id.item_normal_text);
            this.listener = listener;
            tvTime.setGravity(Gravity.CENTER);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClick(v,getAdapterPosition());
            }
        }
    }
}

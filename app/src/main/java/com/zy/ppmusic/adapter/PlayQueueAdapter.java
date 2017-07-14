package com.zy.ppmusic.adapter;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zy.ppmusic.R;

import java.util.List;
import java.util.Locale;

public class PlayQueueAdapter extends RecyclerView.Adapter{
    private List<MediaBrowserCompat.MediaItem> mData;
    private OnQueueItemClickListener onQueueItemClickListener;

    public PlayQueueAdapter(List<MediaBrowserCompat.MediaItem> mData) {
        this.mData = mData;
    }

    public void setData(List<MediaBrowserCompat.MediaItem> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    public void setOnQueueItemClickListener(OnQueueItemClickListener onQueueItemClickListener) {
        this.onQueueItemClickListener = onQueueItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new PlayQueueHolder(LayoutInflater.
                from(viewGroup.getContext()).inflate(R.layout.item_play_queue,viewGroup,false),onQueueItemClickListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if(viewHolder instanceof PlayQueueHolder){
            PlayQueueHolder holder = (PlayQueueHolder) viewHolder;
            MediaDescriptionCompat description = mData.get(i).getDescription();
            holder.tvSubTitle.setText(description.getSubtitle());
            holder.tvTitle.setText(description.getTitle());
            holder.tvPosition.setText(String.format(Locale.CHINA,"%02d",(i+1)));
            holder.tvSubTitle.setTag(mData.get(i));
        }
    }

    @Override
    public int getItemCount() {
        if (mData == null) {
            return 0;
        }
        return mData.size();
    }

    private class PlayQueueHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView tvTitle;
        private TextView tvSubTitle;
        private TextView tvPosition;
        private OnQueueItemClickListener onQueueItemClickListener;

        private PlayQueueHolder(View itemView,OnQueueItemClickListener l) {
            super(itemView);
            this.onQueueItemClickListener = l;
            itemView.setOnClickListener(this);
            tvSubTitle = (TextView) itemView.findViewById(R.id.queue_item_display_sub_title);
            tvTitle = (TextView) itemView.findViewById(R.id.queue_item_display_title);
            tvPosition = (TextView) itemView.findViewById(R.id.queue_item_position);
        }

        @Override
        public void onClick(View v) {
            if (onQueueItemClickListener != null) {
                onQueueItemClickListener.onItemClick(tvSubTitle.getTag(),getAdapterPosition());
            }
        }
    }

    public interface OnQueueItemClickListener{
        void onItemClick(Object obj,int position);
    }
}

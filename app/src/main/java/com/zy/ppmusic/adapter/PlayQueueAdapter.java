package com.zy.ppmusic.adapter;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zy.ppmusic.R;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PlayQueueAdapter extends RecyclerView.Adapter {
    private List<MediaSessionCompat.QueueItem> mData;
    private OnQueueItemClickListener onQueueItemClickListener;
    private OnDelQueueItemListener onDelListener;
    private int selectIndex;

    public int getSelectIndex() {
        return selectIndex;
    }

    public void setSelectIndex(int selectIndex) {
        this.selectIndex = selectIndex;
    }

    public PlayQueueAdapter() {
    }

    public void setData(List<MediaSessionCompat.QueueItem> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    public void childMoveTo(int fromPosition, int toPosition) {
        Collections.swap(mData,fromPosition,toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void setOnDelListener(OnDelQueueItemListener onDelListener) {
        this.onDelListener = onDelListener;
    }

    public void setOnQueueItemClickListener(OnQueueItemClickListener onQueueItemClickListener) {
        this.onQueueItemClickListener = onQueueItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new PlayQueueHolder(LayoutInflater.
                from(viewGroup.getContext()).inflate(R.layout.item_play_queue, viewGroup
                , false), onQueueItemClickListener, onDelListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof PlayQueueHolder) {
            PlayQueueHolder holder = (PlayQueueHolder) viewHolder;
            MediaDescriptionCompat description = mData.get(i).getDescription();
            holder.tvSubTitle.setText(description.getSubtitle());
            holder.tvTitle.setText(description.getTitle());
            if (selectIndex == i) {
                holder.ivPlayingFlag.setVisibility(View.VISIBLE);
                holder.tvPosition.setVisibility(View.GONE);
            } else {
                holder.ivPlayingFlag.setVisibility(View.GONE);
                holder.tvPosition.setVisibility(View.VISIBLE);
                holder.tvPosition.setText(String.format(Locale.CHINA, "%2d", (i + 1)));
            }
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

    private class PlayQueueHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView tvTitle;
        private TextView tvSubTitle;
        private TextView tvPosition;
        private ImageView ivDel;
        private ImageView ivPlayingFlag;
        private OnQueueItemClickListener onQueueItemClickListener;
        private OnDelQueueItemListener delL;

        private PlayQueueHolder(View itemView, OnQueueItemClickListener l, OnDelQueueItemListener dl) {
            super(itemView);
            this.onQueueItemClickListener = l;
            this.delL = dl;
            itemView.setOnClickListener(this);
            tvSubTitle = (TextView) itemView.findViewById(R.id.queue_item_display_sub_title);
            tvTitle = (TextView) itemView.findViewById(R.id.queue_item_display_title);
            tvPosition = (TextView) itemView.findViewById(R.id.queue_item_position);
            ivDel = (ImageView) itemView.findViewById(R.id.queue_item_del);
            ivPlayingFlag = (ImageView) itemView.findViewById(R.id.queue_item_playing_flag_iv);
            ivDel.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.queue_item_del) {
                if (delL != null) {
                    delL.onDel(getAdapterPosition());
                }
            } else {
                if (onQueueItemClickListener != null) {
                    onQueueItemClickListener.onItemClick(tvSubTitle.getTag(), getAdapterPosition());
                }
            }
        }
    }

    public interface OnQueueItemClickListener {
        void onItemClick(Object obj, int position);
    }

    public interface OnDelQueueItemListener {
        void onDel(int position);
    }
}

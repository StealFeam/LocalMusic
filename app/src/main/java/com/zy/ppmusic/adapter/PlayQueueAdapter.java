package com.zy.ppmusic.adapter;

import static com.zy.ppmusic.utils.UIUtilsKt.getString;

import androidx.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.adapter.base.AbstractSingleTypeAdapter;
import com.zy.ppmusic.adapter.base.ExpandableViewHolder;
import com.zy.ppmusic.adapter.base.OnItemViewClickListener;
import com.zy.ppmusic.adapter.base.OnItemViewLongClickListener;
import com.zy.ppmusic.utils.StringUtils;
import com.zy.ppmusic.utils.UIUtilsKt;

import java.util.List;
import java.util.Locale;

/**
 * 播放列表
 *
 * @author ZY
 */
public class PlayQueueAdapter extends AbstractSingleTypeAdapter {
    private List<MediaSessionCompat.QueueItem> mData;
    private OnItemViewClickListener mItemClickListener;
    private OnItemViewLongClickListener mItemLongClickListener;
    private int selectIndex;

    public int getSelectIndex() {
        return selectIndex;
    }

    public void setSelectIndex(int selectIndex) {
        this.selectIndex = selectIndex;
    }

    public PlayQueueAdapter() { }

    public void setData(List<MediaSessionCompat.QueueItem> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    public void setItemLongClickListener(OnItemViewLongClickListener l) {
        this.mItemLongClickListener = l;
    }

    public void setItemClickListener(OnItemViewClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    @Override
    public int getItemLayoutId() {
        return R.layout.item_play_queue;
    }

    @Override
    public void setupItemData(ExpandableViewHolder holder, int position) {
        MediaDescriptionCompat description = mData.get(position).getDescription();
        TextView tvSubTitle = holder.getView(R.id.queue_item_display_sub_title);
        tvSubTitle.setTag(R.id.ignore_theme_color, true);
        tvSubTitle.setText(StringUtils.ifEmpty(String.valueOf(description.getSubtitle()),
                getString(R.string.unknown_author)));
        TextView tvTitle = holder.getView(R.id.queue_item_display_title);
        tvTitle.setTag(R.id.ignore_theme_color, true);
        tvTitle.setText(StringUtils.ifEmpty(String.valueOf(description.getTitle())
                , getString(R.string.unknown_name)));
        TextView tvPosition = holder.getView(R.id.queue_item_position);
        tvPosition.setTag(R.id.ignore_theme_color, true);
        if (selectIndex == position) {
            holder.getView(R.id.queue_item_selected_line).setVisibility(View.VISIBLE);
        } else {
            holder.getView(R.id.queue_item_selected_line).setVisibility(View.INVISIBLE);
        }
        holder.getView(R.id.queue_item_selected_line).setBackgroundColor(UIUtilsKt.getThemeColor());
        tvPosition.setText(String.format(Locale.CHINA, "%2d", (position + 1)));
        tvSubTitle.setTag(mData.get(position));

        holder.getView(R.id.queue_item_del).setTag(R.id.ignore_theme_color, true);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.mData = null;
    }

    @Override
    public int itemCount() {
        if (mData == null) {
            return 0;
        }
        return mData.size();
    }

    @Override
    public void bindHolderData(ExpandableViewHolder holder, int viewType) {
        super.bindHolderData(holder, viewType);
        holder.attachOnLongClickListener(mItemLongClickListener, holder.itemView);
        holder.attachOnClickListener(mItemClickListener, holder.itemView, holder.getView(R.id.queue_item_del));
    }

}

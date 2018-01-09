package com.zy.ppmusic.adapter;

import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.adapter.base.AbstractSingleTypeAdapter;
import com.zy.ppmusic.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhiTouPC
 * @date 2018/1/9
 */

public class PermissionAdapter extends AbstractSingleTypeAdapter {
    private List<String> permissionList;

    public PermissionAdapter(List<String> permissionList) {
        this.permissionList = new ArrayList<>();
        if (permissionList != null) {
            this.permissionList.addAll(permissionList);
        }
    }

    @Override
    public int getItemLayoutId() {
        return R.layout.item_permission_dialog;
    }

    @Override
    public void setupItemData(ExpandableViewHolder holder, int position) {
        TextView nameView = holder.getView(R.id.tv_permission_name);
        nameView.setText(StringUtils.Companion.ifEmpty(this.permissionList.get(position), "unknown"));
    }

    @Override
    public int itemCount() {
        return this.permissionList.size();
    }
}

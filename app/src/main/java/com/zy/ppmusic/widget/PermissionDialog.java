package com.zy.ppmusic.widget;

import android.app.Activity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.adapter.PermissionAdapter;
import com.zy.ppmusic.utils.StringUtils;
import com.zy.ppmusic.utils.UIUtils;

import java.util.List;

/**
 * @author ZhiTouPC
 * @date 2018/1/8
 */
public class PermissionDialog {
    private AppCompatDialog mDialog;
    private String title;
    private String sureTitle;
    private List<String> permissions;
    private ViewGroup mContentView;

    private PermissionDialog(Builder mParams) {
        this.title = mParams.title;
        this.sureTitle = mParams.sureTitle;
        this.permissions = mParams.permissions;
    }

    public void show(Activity activity, final View.OnClickListener l) {
        this.mContentView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.layout_permission_dialog, null);
        TextView titleView = mContentView.findViewById(R.id.tv_permission_title);
        titleView.setText(StringUtils.Companion.ifEmpty(title, activity.getString(R.string.app_name)));
        RecyclerView recyclerView = mContentView.findViewById(R.id.recycler_permission_dialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        PermissionAdapter permissionAdapter = new PermissionAdapter(permissions);
        recyclerView.setAdapter(permissionAdapter);
        Button btnSure = mContentView.findViewById(R.id.btn_sure);
        btnSure.setText(StringUtils.Companion.ifEmpty(sureTitle, activity.getString(R.string.app_name)));
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.cancel();
                }
                if (l != null) {
                    l.onClick(v);
                }
            }
        });
        mDialog = new AppCompatDialog(activity, R.style.TransDialog);
        WindowManager windowManager = activity.getWindowManager();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int width = (metrics.widthPixels * 2 / 3);
        width = width > UIUtils.dp2px(activity, 250) ? width : UIUtils.dp2px(activity, 250);
        mDialog.setContentView(mContentView, new ViewGroup.LayoutParams(width,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
    }

    public void cancelDialog(){
        if(mDialog != null && mDialog.isShowing()){
            mDialog.cancel();
        }
    }


    public static class Builder {
        private String title;
        private List<String> permissions;
        private String sureTitle;

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setPermissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder setSureTitle(String sureTitle) {
            this.sureTitle = sureTitle;
            return this;
        }

        public PermissionDialog build() {
            PermissionDialog dialog = new PermissionDialog(this);
            return dialog;
        }
    }
}

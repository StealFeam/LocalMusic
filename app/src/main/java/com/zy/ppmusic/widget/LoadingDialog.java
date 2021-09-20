package com.zy.ppmusic.widget;

import android.content.Context;
import androidx.appcompat.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.zy.ppmusic.R;

import java.lang.ref.WeakReference;

/**
 * @author stealfeam
 * @date 2018/1/16
 */
public class LoadingDialog extends AppCompatDialog {
    private View mContentView;
    private final WeakReference<Context> mContextWeak;

    public LoadingDialog(Context context) {
        super(context, R.style.TransDialog);
        this.mContextWeak = new WeakReference<>(context);
    }

    @Override
    public void show() {
        showLoadingView();
        super.show();
    }

    private void showLoadingView() {
        if (mContextWeak.get() == null) {
            hideLoadingView();
            return;
        }
        if (mContentView == null) {
            mContentView = LayoutInflater.from(mContextWeak.get()).inflate(R.layout.loading_layout, null);
            setContentView(mContentView);
        }
        mContentView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hide() {
        hideLoadingView();
        super.hide();
    }

    @Override
    public void cancel() {
        hideLoadingView();
        super.cancel();
    }

    private void hideLoadingView() {
        if (mContentView != null) {
            mContentView.setVisibility(View.GONE);
        }
    }
}

package com.zy.ppmusic.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zy.ppmusic.R;

/**
 * @author y-slience
 * @date 2018/3/7
 */

public class LoadingDialogFragment extends DialogFragment {

    public LoadingDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getContext() == null && getActivity() == null) {
            throw new NullPointerException();
        }
        Context context = getContext() == null ? getActivity() : getContext();
        return new AppCompatDialog(context, R.style.TransDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_wave_loading, container);
    }


}

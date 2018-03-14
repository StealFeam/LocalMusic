package com.zy.ppmusic.widget;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author y-slience
 * @date 2018/3/7
 */

public class LoadingDialogFragment extends DialogFragment{
    private static final String TAG = "LoadingDialogFragment";
    private Builder mBuilder;

    private static LoadingDialogFragment create(Builder mBuilder){
        Bundle extra = new Bundle();
        extra.putParcelable(TAG,mBuilder);
        LoadingDialogFragment fragment = new LoadingDialogFragment();
        fragment.setArguments(extra);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public static class Builder implements Parcelable{
        private String content;
        private boolean isCancelable;

        private Builder(Parcel in) {
            content = in.readString();
            isCancelable = in.readByte() != 0;
        }

        public static final Creator<Builder> CREATOR = new Creator<Builder>() {
            @Override
            public Builder createFromParcel(Parcel in) {
                return new Builder(in);
            }

            @Override
            public Builder[] newArray(int size) {
                return new Builder[size];
            }
        };

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isCancelable() {
            return isCancelable;
        }

        public void setCancelable(boolean cancelable) {
            isCancelable = cancelable;
        }

        public LoadingDialogFragment create(){
            return LoadingDialogFragment.create(this);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(content);
            dest.writeByte((byte) (isCancelable ? 1 : 0));
        }
    }
}

package com.zy.ppmusic.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.zy.ppmusic.ui.MediaActivity;
import com.zy.ppmusic.R;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class ScanFileFragment extends Fragment {
    public static final String TAG = "ScanFileFragment";
    private RecyclerView mResultRecycler;
    private Button mBtnAdd;
    private ArrayList<String> mResult;
    private WeakReference<MediaActivity> mActivityWeak;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivityWeak = new WeakReference<MediaActivity>((MediaActivity) getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_scan_file_content, null);
        mBtnAdd = (Button) rootView.findViewById(R.id.btn_add_all);
        mBtnAdd.setText("正在扫描...");
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mResult != null) {
                    DataTransform.getInstance().transFormData(getContext(), mResult);
                } else {
                    showMsg("请等候扫描完成");
                }
            }
        });
        ScanMusicFile.getInstance().scanMusicFile(getActivity()).setOnScanComplete(new ScanMusicFile.OnScanComplete() {
            @Override
            protected void onComplete(ArrayList<String> paths) {
                mBtnAdd.setText(String.format(Locale.CHINA,"将%d首歌曲替换到播放列表中",paths.size()));
                mResult = paths;
            }

            @Override
            protected void onCountChange(int size) {
                mBtnAdd.setText(String.format(Locale.CHINA, "已扫描到%d首歌曲", size));
            }
        });
        return rootView;
    }

    public void showMsg(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

}

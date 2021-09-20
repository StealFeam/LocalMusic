package com.zy.ppmusic.mvp.presenter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.zy.ppmusic.entity.ScanResultEntity;
import com.zy.ppmusic.mvp.contract.IBLActivityContract;
import com.zy.ppmusic.mvp.model.BlActivityModelImpl;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author ZY
 */
@SuppressLint("MissingPermission")
public class BlActivityPresenter extends IBLActivityContract.AbstractBLActivityPresenter {
    private final BluetoothAdapter mBlueAdapter;
    private final Context context;
    private BluetoothA2dp mBlueA2dp;
    private boolean isNeedRe = false;

    public BlActivityPresenter(IBLActivityContract.IBLActivityView mView) {
        super(mView);
        context = mView.getContext();
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected IBLActivityContract.IBLActivityModel createModel() {
        return new BlActivityModelImpl();
    }

    private void initBlueA2dp() {
        mBlueAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothA2dp.A2DP) {
                    mBlueA2dp = (BluetoothA2dp) proxy;
                    if (isNeedRe) {
                        List<ScanResultEntity> exitDevices = mModel.getExitsDevices(mBlueAdapter, mBlueA2dp);
                        if (mView.get() != null) {
                            mView.get().setExitsDevices(exitDevices);
                        }
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                mBlueA2dp = null;
            }
        }, BluetoothA2dp.A2DP);
    }

    @Override
    public void getExitsDevices() {
        if (mBlueA2dp != null) {
            if (isDiscovering()) {
                cancelDiscovery();
            }
            List<ScanResultEntity> exitDevices = mModel.getExitsDevices(mBlueAdapter, mBlueA2dp);
            if (mView.get() != null) {
                mView.get().setExitsDevices(exitDevices);
            }
            startDiscovery();
        } else {
            isNeedRe = true;
            initBlueA2dp();
        }
    }


    @Override
    public boolean startDiscovery() {
        cancelDiscovery();
        return mBlueAdapter != null && mBlueAdapter.startDiscovery();
    }

    @Override
    public boolean cancelDiscovery() {
        return mBlueAdapter != null && mBlueAdapter.isDiscovering() && mBlueAdapter.cancelDiscovery();
    }

    @Override
    public boolean connectDevice(@NotNull BluetoothDevice device) {
        return mModel.connectDevice(device, mBlueAdapter, mBlueA2dp);
    }

    @Override
    public boolean disconnectDevice(@NotNull BluetoothDevice device) {
        return mModel.disconnectDevice(device, mBlueAdapter, mBlueA2dp);
    }

    @Override
    public int getConnectState(@NotNull BluetoothDevice device) {
        if (mBlueA2dp != null) {
            return mBlueA2dp.getConnectionState(device);
        }
        return 0;
    }

    @Override
    public boolean isSupportBl() {
        return mBlueAdapter != null;
    }

    @Override
    public boolean isEnable() {
        return mBlueAdapter != null && mBlueAdapter.isEnabled();
    }

    @Override
    public void enable() {
        if (mBlueAdapter != null) {
            mBlueAdapter.enable();
            initBlueA2dp();
        }
    }


    @Override
    public void disable() {
        if (mBlueAdapter != null) {
            mBlueAdapter.disable();
        }
    }

    @Override
    public void detachViewAndModel() {
        super.detachViewAndModel();
        if (mBlueAdapter == null) {
            return;
        }
        if (mBlueAdapter.isDiscovering()) {
            mBlueAdapter.cancelDiscovery();
        }
        mBlueAdapter.closeProfileProxy(BluetoothA2dp.A2DP, mBlueA2dp);
    }

    @Override
    public boolean isConnected(@NotNull BluetoothDevice device) {
        System.err.println("已连接的蓝牙设备。。。" + mBlueA2dp.getConnectedDevices().toString());
        return mBlueA2dp != null && mBlueA2dp.getConnectedDevices().contains(device);
    }

    @Override
    public List<BluetoothDevice> getConnectDevice() {
        if (mBlueA2dp != null) {
            return mBlueA2dp.getConnectedDevices();
        }
        return null;
    }


    @Override
    public Set<BluetoothDevice> getBondDevice() {
        if (mBlueA2dp == null) {
            return null;
        }
        return mBlueAdapter.getBondedDevices();
    }

    @Override
    public boolean isDiscovering() {
        return mBlueAdapter != null && mBlueAdapter.isDiscovering();
    }


    @Override
    public boolean removeBondDevice(@NotNull BluetoothDevice device) {
        return mModel.removeBond(device);
    }
}

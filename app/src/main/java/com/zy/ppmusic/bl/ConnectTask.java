package com.zy.ppmusic.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * @author ZY
 */
public class ConnectTask extends AsyncTask<Void,Void,Boolean>{
    private static final String TAG = "ConnectTask";
    //这条是蓝牙串口通用的UUID，不要更改
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device;

    public ConnectTask(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            BluetoothSocket insecureRfcommSocketToServiceRecord = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            insecureRfcommSocketToServiceRecord.connect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        Log.e(TAG, "onPostExecute: connect "+(aBoolean?"成功":"失败"));
    }
}

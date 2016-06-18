package com.example.andy.SolarMonitor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class Bluetoothservice extends Service {

    private final Handler mHandler;
    private final BluetoothAdapter mAdapter;


    public Bluetoothservice(Context context, Handler handler) {
        mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

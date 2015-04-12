package com.alley.android.ppi.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PPISyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static PropertyPriceSyncAdapter sPropertyPriceSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("PPISyncService", "onCreate - PPISyncService");
        synchronized (sSyncAdapterLock) {
            if (sPropertyPriceSyncAdapter == null) {
                sPropertyPriceSyncAdapter = new PropertyPriceSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sPropertyPriceSyncAdapter.getSyncAdapterBinder();
    }
}
package com.testoffline.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * This is the "Morning Guard".
 * It wakes up ONLY when the phone restarts (BOOT_COMPLETED).
 * Its job is to restart both our 15-min backup and our "Watchman" service.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Phone rebooted. Restarting services.");

            // 1. Re-schedule the 15-minute backup worker
            SyncWorker.schedulePeriodicSync(context);

            // 2. Restart the Persistent "Watchman" Service
            Intent serviceIntent = new Intent(context, PersistentSyncService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}

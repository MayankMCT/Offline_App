package com.testoffline.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This receiver's ONLY job is to re-schedule the periodic sync
 * when the phone is rebooted. This is critical for reliability.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootCompletedReceiver", "Phone rebooted. Re-scheduling periodic sync.");
            SyncWorker.schedulePeriodicSync(context);
        }
    }
}

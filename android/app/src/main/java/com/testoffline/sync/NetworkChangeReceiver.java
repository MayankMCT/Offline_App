package com.testoffline.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * BroadcastReceiver that listens for network connectivity changes
 * This works even when the app is completely killed
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Network change detected!");

        // Check if we're now connected to internet
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Log.d(TAG, "Device is now ONLINE - triggering sync work");

            // Schedule a one-time sync work using WorkManager
            SyncWorker.scheduleOneTimeSync(context);
        } else {
            Log.d(TAG, "Device is OFFLINE");
        }
    }
}

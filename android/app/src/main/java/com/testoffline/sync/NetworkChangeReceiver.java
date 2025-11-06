package com.testoffline.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                Log.d("NetworkChangeReceiver", "Network connected (App is open/background). Scheduling ONE-TIME sync.");
                // Schedule the sync job using WorkManager
                // This gives the "instant" feel when app is not fully killed.
                SyncWorker.scheduleOneTimeSync(context);
            } else {
                Log.d("NetworkChangeReceiver", "Network disconnected.");
            }
        }
    }
}

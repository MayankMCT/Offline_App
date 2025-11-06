package com.testoffline.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

/**
 * This is the 24/7 Foreground Service you asked for.
 * It runs even if the app is killed and INSTANTLY listens for network changes.
 * This requires a persistent notification.
 */
public class PersistentSyncService extends Service {

    private static final String TAG = "PersistentSyncService";
    private static final String CHANNEL_ID = "PersistentSyncServiceChannel";
    private static final int SERVICE_NOTIFICATION_ID = 1; // Must be different from SyncWorker's ID

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PersistentSyncService onCreate");
        createNotificationChannel();

        // This is the persistent notification that is REQUIRED by Android
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Offline App")
                .setContentText("Sync service is running.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority so it's less intrusive
                .build();

        // This is what makes it a Foreground Service
        startForeground(SERVICE_NOTIFICATION_ID, notification);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // This is the modern network listener. It will fire instantly
        // as long as this service is running.
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.i(TAG, "NETWORK AVAILABLE! Triggering one-time sync.");
                // We have internet! Trigger the SyncWorker to do the actual work.
                SyncWorker.scheduleOneTimeSync(getApplicationContext());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.i(TAG, "Network lost.");
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "PersistentSyncService onStartCommand");
        // If the service is killed by the system, restart it
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PersistentSyncService onDestroy");
        // Unregister the network callback when the service is destroyed
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Persistent Sync Service", // Channel name
                    NotificationManager.IMPORTANCE_LOW // Use LOW to ensure it's a silent, persistent notification
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}

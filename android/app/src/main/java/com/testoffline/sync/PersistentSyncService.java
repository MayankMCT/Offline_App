package com.testoffline.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

/**
 * This is the "Watchman" service.
 * It runs 24/7 as a Foreground Service to listen for network changes.
 * Android is very unlikely to kill this service because it shows a persistent notification.
 */
public class PersistentSyncService extends Service {

    private static final String TAG = "PersistentSyncService";
    public static final int NOTIFICATION_ID = 1; // Different ID from SyncWorker
    public static final String CHANNEL_ID = "PersistentSyncChannel";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Watchman Service onCreate");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        createNotificationChannel();

        // This callback fires when network is available
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.i(TAG, "NETWORK AVAILABLE! Triggering instant sync.");
                // Tell the JavaScript side (if it's open)
                BackgroundSyncModule.sendNetworkChangeEvent("online");
                // Schedule an immediate sync task with WorkManager
                SyncWorker.scheduleOneTimeSync(getApplicationContext());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.i(TAG, "NETWORK LOST.");
                // Tell the JavaScript side (if it's open)
                BackgroundSyncModule.sendNetworkChangeEvent("offline");
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Watchman Service onStartCommand - Starting Foreground Service.");

        // Create the persistent notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Offline App")
                .setContentText("Sync service is running in background")
                .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
                .setContentIntent(pendingIntent)
                .setOngoing(true) // ***[CHANGE]*** Make it non-swipeable
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // ***[CHANGE]*** Set default priority
                .build();

        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, notification);

        // Register the network callback
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        // If the service is killed, restart it
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Watchman Service onDestroy - Unregistering network callback.");
        // Clean up
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Persistent Sync Service",
                    NotificationManager.IMPORTANCE_DEFAULT // ***[CHANGE]*** Default importance
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}

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
import android.net.NetworkCapabilities; // <-- Import this
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class PersistentSyncService extends Service {

    private static final String TAG = "PersistentSyncService";
    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "PersistentSyncChannel";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isCallbackRegistered = false; // <-- Added to prevent re-registering

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Watchman Service onCreate");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        createNotificationChannel();

        // **FIX 4: Moved Network Callback creation to onCreate**
        // This ensures it's only created once when the service is created.
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.i(TAG, "NETWORK AVAILABLE! Triggering instant sync.");
                BackgroundSyncModule.sendNetworkChangeEvent("online");
                SyncWorker.scheduleOneTimeSync(getApplicationContext());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.i(TAG, "NETWORK LOST.");
                BackgroundSyncModule.sendNetworkChangeEvent("offline");
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Watchman Service onStartCommand - Starting Foreground Service.");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Offline App")
                .setContentText("Sync service is running in background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                // **FIX 2: Set priority to LOW**
                // This makes it less intrusive, matching the channel.
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // This is correct! Makes it non-swipeable.
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // **FIX 4: Only register the callback if it's not already registered**
        if (!isCallbackRegistered) {
            // **FIX 3: Make the request specific to Internet access**
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            isCallbackRegistered = true;
            Log.d(TAG, "Network callback registered.");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Watchman Service onDestroy - Unregistering network callback.");
        // Clean up
        if (connectivityManager != null && networkCallback != null && isCallbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isCallbackRegistered = false;
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
                    // **FIX 1: This is the MAIN FIX for the swipeable issue.**
                    // Must be LOW for a non-intrusive, non-swipeable notification.
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Notification for the persistent background service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}

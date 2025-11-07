package com.testoffline.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.testoffline.sync.MainActivity; // Make sure this import is correct
import com.testoffline.sync.R; // Make sure this import is correct

public class BackgroundSyncModule extends ReactContextBaseJavaModule {
    private static final String TAG = "BackgroundSyncModule";
    private static ReactApplicationContext reactContext;

    public BackgroundSyncModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @NonNull
    @Override
    public String getName() {
        return "BackgroundSyncModule";
    }

    /**
     * --- THIS IS YOUR "OPEN SETTINGS" FIX ---
     * It now uses the application context and adds FLAG_ACTIVITY_NEW_TASK
     * to reliably open the settings page from anywhere.
     */
    @ReactMethod
    public void requestIgnoreBatteryOptimizations(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = getReactApplicationContext();
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "Requesting battery optimization exemption.");
                try {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    // **THIS IS THE FIX:**
                    // We add FLAG_ACTIVITY_NEW_TASK to allow starting an Activity
                    // from a non-Activity context (like this module).
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    promise.resolve("requested");
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting battery optimization", e);
                    promise.reject("E_ERROR", e.getMessage());
                }
            } else {
                Log.d(TAG, "Battery optimization already ignored.");
                promise.resolve("already_ignored");
            }
        } else {
            // Not needed for older Android
            promise.resolve("not_needed");
        }
    }

    /**
     * --- THIS IS YOUR "UPDATE NOTIFICATION" FIX ---
     * This new method allows JS to update the persistent notification text.
     */
    @ReactMethod
    public void updateNotification(String text, boolean ongoing, Promise promise) {
        try {
            Context context = getReactApplicationContext();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                promise.reject("E_NO_MANAGER", "NotificationManager is null");
                return;
            }

            Intent notificationIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(context, PersistentSyncService.CHANNEL_ID)
                .setContentTitle("Offline App")
                .setContentText(text) // Set the new text from JavaScript
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(ongoing) // Set sticky status from JavaScript
                .build();

            notificationManager.notify(PersistentSyncService.NOTIFICATION_ID, notification);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update notification", e);
            promise.reject("E_NOTIF_UPDATE", e.getMessage());
        }
    }

    /**
     * --- THIS IS YOUR "UPDATE NOTIFICATION" FIX ---
     * This new method resets the notification to its default state.
     */
    @ReactMethod
    public void resetNotification(Promise promise) {
        // Reset to "Service is running" and make it non-swipeable again
        updateNotification("Service is running in background", true, promise);
    }


    /**
     * Manually trigger sync (for testing)
     */
    @ReactMethod
    public void triggerSync(Promise promise) {
        try {
            Context context = getReactApplicationContext();
            SyncWorker.scheduleOneTimeSync(context);
            Log.d(TAG, "✅ Sync triggered manually");
            promise.resolve("triggered");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to trigger sync: " + e.getMessage());
            promise.reject("ERROR", e.getMessage());
        }
    }

    /**
     * Send event to JavaScript when network changes detected
     */
    public static void sendNetworkChangeEvent(String status) {
        if (reactContext != null) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("NetworkStatusChanged", status);
        }
    }
}

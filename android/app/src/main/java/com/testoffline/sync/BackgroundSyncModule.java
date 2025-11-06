package com.testoffline.sync;

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
     * THIS IS THE NEW FUNCTION
     * It checks if battery optimization is disabled.
     * If not, it opens the system dialog to ask the user to disable it.
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
                    // We must use getCurrentActivity to launch this system dialog
                    if (getCurrentActivity() != null) {
                        getCurrentActivity().startActivity(intent);
                        promise.resolve("requested");
                    } else {
                        promise.reject("E_NO_ACTIVITY", "Current activity is null, cannot request permission.");
                    }
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

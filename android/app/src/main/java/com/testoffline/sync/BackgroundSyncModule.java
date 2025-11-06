package com.testoffline.sync;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import androidx.annotation.NonNull;

/**
 * Native module to handle background sync
 */
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
     * Initialize background sync worker
     */
    @ReactMethod
    public void initialize(Promise promise) {
        try {
            Log.d(TAG, "✅ Background sync module initialized");
            promise.resolve("initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize: " + e.getMessage());
            promise.reject("ERROR", e.getMessage());
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

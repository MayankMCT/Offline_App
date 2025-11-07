package com.testoffline.sync;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class BackgroundSyncModule extends ReactContextBaseJavaModule {
  private static final String TAG = "BackgroundSyncModule";
  private static ReactApplicationContext reactContext;

  public BackgroundSyncModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
  }

  @NonNull
  @Override public String getName() { return "BackgroundSyncModule"; }

  // Battery optimization helper (optional)
  @ReactMethod
  public void requestIgnoreBatteryOptimizations(Promise promise) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PowerManager pm = (PowerManager) reactContext.getSystemService(Context.POWER_SERVICE);
        String pkg = reactContext.getPackageName();
        if (pm != null && !pm.isIgnoringBatteryOptimizations(pkg)) {
          Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
          i.setData(Uri.parse("package:" + pkg));
          if (getCurrentActivity() != null) {
            getCurrentActivity().startActivity(i);
            promise.resolve("requested");
            return;
          } else {
            promise.reject("E_NO_ACTIVITY", "Current activity is null");
            return;
          }
        }
      }
      promise.resolve("already_ignored_or_not_needed");
    } catch (Exception e) {
      Log.e(TAG, "requestIgnoreBatteryOptimizations error", e);
      promise.reject("E_ERROR", e.getMessage());
    }
  }

  // ✅ Always-on foreground listener: start
  @ReactMethod
  public void startPersistentService(Promise promise) {

    
    try {
      Context ctx = getReactApplicationContext();
      Intent svc = new Intent(ctx, PersistentSyncService.class);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ctx.startForegroundService(svc);
      } else {
        ctx.startService(svc);
      }
      Log.d(TAG, "PersistentSyncService started");
      promise.resolve(true);
    } catch (Exception e) {
      Log.e(TAG, "startPersistentService failed", e);
      promise.reject("START_SERVICE_FAILED", e);
    }
  }

  // ✅ Always-on foreground listener: stop
  @ReactMethod
  public void stopPersistentService(Promise promise) {
    try {
      Context ctx = getReactApplicationContext();
      Intent svc = new Intent(ctx, PersistentSyncService.class);
      boolean stopped = ctx.stopService(svc);
      Log.d(TAG, "PersistentSyncService stop = " + stopped);
      promise.resolve(stopped);
    } catch (Exception e) {
      Log.e(TAG, "stopPersistentService failed", e);
      promise.reject("STOP_SERVICE_FAILED", e);
    }
  }

  // One-time background sync via WorkManager
  @ReactMethod
  public void triggerSync(Promise promise) {
    try {
      Context ctx = getReactApplicationContext();
      SyncWorker.scheduleOneTimeSync(ctx);
      Log.d(TAG, "One-time sync scheduled");
      promise.resolve("triggered");
    } catch (Exception e) {
      Log.e(TAG, "triggerSync error", e);
      promise.reject("ERROR", e.getMessage());
    }
  }

  // (optional) JS events
  public static void sendNetworkChangeEvent(String status) {
    if (reactContext != null) {
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit("NetworkStatusChanged", status);
    }
  }
}

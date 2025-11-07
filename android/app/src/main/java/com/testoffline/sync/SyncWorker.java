package com.testoffline.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
  private static final String TAG = "SyncWorker";
  private static final String SYNC_WORK_NAME = "offline_sync_work";
  private static final String PERIODIC_SYNC_NAME = "offline_periodic_sync";

  public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    Log.d(TAG, "🔄 SyncWorker started - performing background sync");
    try {
      Context context = getApplicationContext();
      // Kick Headless JS service to run BackgroundSync
      Intent serviceIntent = new Intent(context, SyncHeadlessTaskService.class);
      context.startService(serviceIntent);

      // Give JS some time (actual timeout enforced by HeadlessJsTaskConfig = 30s)
      Thread.sleep(3000);
      Log.d(TAG, "✅ Headless sync service started");
      return Result.success();
    } catch (Exception e) {
      Log.e(TAG, "❌ Sync failed: " + e.getMessage());
      return Result.retry();
    }
  }

  public static void scheduleOneTimeSync(Context context) {
    Log.d(TAG, "📅 Scheduling one-time sync work");
    Constraints constraints = new Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build();

    OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
      .setConstraints(constraints)
      .build();

    WorkManager.getInstance(context)
      .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, req);
  }

  public static void schedulePeriodicSync(Context context) {
    Log.d(TAG, "📅 Scheduling periodic sync (15 min)");
    Constraints constraints = new Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build();

    PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(SyncWorker.class,
      java.time.Duration.ofMinutes(15))
      .setConstraints(constraints)
      .build();

    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(PERIODIC_SYNC_NAME, ExistingPeriodicWorkPolicy.UPDATE, req);
  }
}

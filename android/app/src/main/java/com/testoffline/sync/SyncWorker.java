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

import com.facebook.react.HeadlessJsTaskService;
import java.util.concurrent.TimeUnit;

/**
 * This is the "Worker". It is now 100% silent.
 * Its ONLY job is to start the SyncHeadlessTaskService.
 * All user-visible notifications are now handled by the BackgroundSyncModule.
 */
public class SyncWorker extends Worker {

    private static final String PERIODIC_SYNC_WORK_TAG = "background-periodic-sync-work";
    private static final String ONE_TIME_SYNC_WORK_TAG = "background-one-time-sync-work";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SyncWorker", "WorkManager task started.");
        Context context = getApplicationContext();

        try {
            // Start the Headless JS task to run the JavaScript sync logic
            Intent service = new Intent(context, SyncHeadlessTaskService.class);
            context.startService(service);
            HeadlessJsTaskService.acquireWakeLockNow(context);
            Log.d("SyncWorker", "HeadlessJsTaskService started silently.");
        } catch (Exception e) {
            Log.e("SyncWorker", "Starting HeadlessJsTaskService failed.", e);
            return Result.failure();
        }

        return Result.success();
    }


    /**
     * Schedules the reliable 15-minute periodic sync.
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        PeriodicWorkRequest syncWorkRequest =
            new PeriodicWorkRequest.Builder(SyncWorker.class,15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP, // KEEP: If work exists, do nothing.
            syncWorkRequest
        );

        Log.d("SyncWorker", "Periodic sync work (15 min) scheduled with WorkManager.");
    }

    /**
     * Schedules an IMMEDIATE one-time sync.
     */
    public static void scheduleOneTimeSync(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_TAG,
            ExistingWorkPolicy.REPLACE, // REPLACE any pending one-time sync
            syncWorkRequest
        );
        Log.d("SyncWorker", "One-time immediate sync work scheduled with WorkManager.");
    }
}

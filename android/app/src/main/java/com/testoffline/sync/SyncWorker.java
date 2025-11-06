package com.testoffline.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.facebook.react.HeadlessJsTaskService;

/**
 * WorkManager Worker that performs the actual sync operation
 * This can run even when the app is killed
 */
public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    private static final String SYNC_WORK_NAME = "offline_sync_work";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 SyncWorker started - performing background sync");

        try {
            Context context = getApplicationContext();

            // Start the headless JS service to perform sync
            Intent serviceIntent = new Intent(context, SyncHeadlessTaskService.class);
            context.startService(serviceIntent);

            Log.d(TAG, "✅ Headless sync service started");

            // Give it some time to complete
            Thread.sleep(5000);

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "❌ Sync failed: " + e.getMessage());
            return Result.retry();
        }
    }

    /**
     * Schedule a one-time sync work
     */
    public static void scheduleOneTimeSync(Context context) {
        Log.d(TAG, "📅 Scheduling one-time sync work");

        // Create constraints - only run when connected to internet
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create one-time work request
        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        // Enqueue the work (replace any existing work with same name)
        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        SYNC_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        syncWork
                );

        Log.d(TAG, "✅ Sync work scheduled");
    }
}

package com.testoffline.sync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.facebook.react.HeadlessJsTaskService;
import java.util.concurrent.TimeUnit; // Import this

public class SyncWorker extends Worker {

    private static final String PERIODIC_SYNC_WORK_TAG = "background-periodic-sync-work";
    private static final String ONE_TIME_SYNC_WORK_TAG = "background-one-time-sync-work";
    private static final String NOTIFICATION_CHANNEL_ID = "BackgroundSyncChannel";
    private static final int NOTIFICATION_ID = 12345;

    private NotificationManager notificationManager;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SyncWorker", "WorkManager task started.");
        Context context = getApplicationContext();

        // Promote this worker to a foreground service to show a notification
        // and ensure the task is not killed by the OS.
        createNotificationChannel();
        ForegroundInfo foregroundInfo = createForegroundInfo("Starting background sync...");
        setForegroundAsync(foregroundInfo);

        try {
            // Start the Headless JS task to run the JavaScript sync logic
            Intent service = new Intent(context, SyncHeadlessTaskService.class);
            context.startService(service);
            HeadlessJsTaskService.acquireWakeLockNow(context);
            Log.d("SyncWorker", "HeadlessJsTaskService started.");

            // We will update the notification to "Sync in progress".
            updateNotification("Sync in progress...");

        } catch (Exception e) {
            Log.e("SyncWorker", "Starting HeadlessJsTaskService failed.", e);
            updateNotification("Sync failed to start.");
            return Result.failure();
        }

        // We return success, but the Headless JS task is still running.
        // A more advanced solution would have the JS task send a message
        // back to native code to update/dismiss the notification.
        return Result.success();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Background Sync",
                NotificationManager.IMPORTANCE_LOW // Use LOW to be less intrusive
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private ForegroundInfo createForegroundInfo(String message) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Offline App")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        return new ForegroundInfo(NOTIFICATION_ID, notification.build());
    }

    private void updateNotification(String message) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Offline App")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false); // Make it dismissible after the update

        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }


    /**
     * Schedules the reliable 15-minute periodic sync.
     * This is our GUARANTEE that sync will run even if app is killed or phone reboots.
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        PeriodicWorkRequest syncWorkRequest =
            new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE, // REPLACE ensures the task is always scheduled with the latest code
            syncWorkRequest
        );

        Log.d("SyncWorker", "Periodic sync work (15 min) scheduled with WorkManager.");
    }

    /**
     * Schedules an IMMEDIATE one-time sync.
     * This is called by NetworkChangeReceiver for the "instant" sync feel.
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

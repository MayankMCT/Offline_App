package com.testoffline.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class SyncForegroundService : Service() {
  companion object {
    private const val TAG = "SyncForegroundService"
    private const val CHANNEL_ID = "offline_sync_channel"
    private const val NOTIF_ID = 102
  }

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate: creating channel & promoting to foreground")
    createNotificationChannel()
    startForeground(NOTIF_ID, buildOngoingNotification("Preparing sync…"))
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand: starting background sync task")

    // Start HeadlessJS to run your JS fullSync()
    val headlessIntent = Intent(applicationContext, SyncHeadlessTaskService::class.java)
    applicationContext.startService(headlessIntent)

    // Update notification while running
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIF_ID, buildOngoingNotification("Sync in progress…"))

    return START_STICKY
  }

  override fun onDestroy() {
    Log.d(TAG, "onDestroy: foreground service destroyed")
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun buildOngoingNotification(text: String): Notification {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    val pi = PendingIntent.getActivity(
      this, 0, launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Offline Sync")
      .setContentText(text)
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setOngoing(true)
      .setContentIntent(pi)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Data Sync",
        NotificationManager.IMPORTANCE_LOW
      )
      channel.description = "Shows progress while syncing data"
      nm.createNotificationChannel(channel)
    }
  }
}

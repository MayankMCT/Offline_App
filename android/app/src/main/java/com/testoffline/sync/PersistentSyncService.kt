package com.testoffline.sync

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.facebook.react.HeadlessJsTaskService
import java.util.concurrent.atomic.AtomicLong

class PersistentSyncService : Service() {

  companion object {
    private const val TAG = "PersistentSyncService"
    private const val CHANNEL_ID = "offline_sync_channel"
    private const val NOTIF_ID = 101
    private const val POLL_MS = 5000L             // ✅ every 5s
    private const val MIN_SYNC_GAP_MS = 3000L     // don't re-fire within 3s
  }

  private var cm: ConnectivityManager? = null
  private var callback: ConnectivityManager.NetworkCallback? = null
  private val handler = Handler(Looper.getMainLooper())
  private var pollingRunnable: Runnable? = null
  private val lastKick = AtomicLong(0)

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate(): starting foreground + listeners")
    createNotifChannel()
    startForeground(NOTIF_ID, buildNotif("Listening for network…"))

    cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // ✅ Network callback (instant when default network becomes available)
    callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        Log.d(TAG, "NetworkCallback.onAvailable → kick sync fast")
        kickHeadlessNow()
      }
    }
    try {
      cm?.registerDefaultNetworkCallback(callback!!)
    } catch (e: Exception) {
      Log.w(TAG, "registerDefaultNetworkCallback failed: ${e.message}")
    }

    // ✅ Polling every 5s as a backup (some OEMs suppress callbacks)
    pollingRunnable = object : Runnable {
      override fun run() {
        try {
          if (isOnline()) {
            Log.d(TAG, "Polling: online → kick headless (if not just done)")
            kickHeadlessNow()
          }
        } catch (_: Exception) { }
        handler.postDelayed(this, POLL_MS)
      }
    }
    handler.postDelayed(pollingRunnable!!, POLL_MS)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand(): service sticky")
    return START_STICKY
  }

  override fun onDestroy() {
    Log.d(TAG, "onDestroy(): unregister + stop polling")
    try { if (callback != null) cm?.unregisterNetworkCallback(callback!!) } catch (_: Exception) {}
    try { handler.removeCallbacksAndMessages(null) } catch (_: Exception) {}
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  // ---- helpers ----

  private fun isOnline(): Boolean {
    val nc = cm?.getNetworkCapabilities(cm?.activeNetwork) ?: return false
    val hasNet = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val validated = if (Build.VERSION.SDK_INT >= 23) {
      nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } else hasNet
    return hasNet && validated
  }

  private fun kickHeadlessNow() {
    val now = System.currentTimeMillis()
    if (now - lastKick.get() < MIN_SYNC_GAP_MS) return
    lastKick.set(now)

    try {
      // Update notif quickly
      notifyText("Sync in progress…")

      // ✅ 1) Start HeadlessJS immediately (fastest path)
      val i = Intent(applicationContext, SyncHeadlessTaskService::class.java)
      applicationContext.startService(i)
      HeadlessJsTaskService.acquireWakeLockNow(applicationContext)
      Log.d(TAG, "HeadlessJS kicked")

      // ✅ 2) Backup: also schedule one-time WorkManager (in case OS throttles)
      try {
        SyncWorker.scheduleOneTimeSync(applicationContext)
      } catch (_: Exception) {}

      // Restore notif text after a short delay
      handler.postDelayed({ notifyText("Listening for network…") }, 2500)
    } catch (e: Exception) {
      Log.w(TAG, "kickHeadlessNow error: ${e.message}")
    }
  }

  private fun notifyText(text: String) {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIF_ID, buildNotif(text))
  }

  private fun buildNotif(text: String): Notification {
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
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun createNotifChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val ch = NotificationChannel(CHANNEL_ID, "Data Sync", NotificationManager.IMPORTANCE_LOW)
      ch.description = "Shows background sync state"
      nm.createNotificationChannel(ch)
    }
  }
}

package com.testoffline.sync

import android.content.Intent
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class ForegroundSyncModule(private val ctx: ReactApplicationContext)
  : ReactContextBaseJavaModule(ctx) {

  override fun getName() = "ForegroundSyncModule"

  @ReactMethod
  fun start(promise: Promise) {
    try {
      val i = Intent(ctx, SyncForegroundService::class.java)
      ctx.startForegroundService(i)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("START_FG_FAIL", e)
    }
  }

  @ReactMethod
  fun stop(promise: Promise) {
    try {
      val i = Intent(ctx, SyncForegroundService::class.java)
      val r = ctx.stopService(i)
      promise.resolve(r)
    } catch (e: Exception) {
      promise.reject("STOP_FG_FAIL", e)
    }
  }
}

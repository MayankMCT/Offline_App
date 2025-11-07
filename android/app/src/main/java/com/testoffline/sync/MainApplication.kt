package com.testoffline.sync

import android.app.Application
import android.content.Intent
import android.content.res.Configuration

import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.ReactHost
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader

import expo.modules.ApplicationLifecycleDispatcher
import expo.modules.ReactNativeHostWrapper

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost = ReactNativeHostWrapper(
    this,
    object : DefaultReactNativeHost(this) {
      override fun getPackages(): List<ReactPackage> {
        val packages = PackageList(this).packages.toMutableList()
        packages.add(BackgroundSyncPackage())
        packages.add(ForegroundSyncPackage())  
        return packages
      }
      override fun getJSMainModuleName() = ".expo/.virtual-metro-entry"
      override fun getUseDeveloperSupport() = BuildConfig.DEBUG
      override val isNewArchEnabled = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      override val isHermesEnabled = BuildConfig.IS_HERMES_ENABLED
    }
  )

  override val reactHost: ReactHost
    get() = ReactNativeHostWrapper.createReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, false)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) { load() }
    ApplicationLifecycleDispatcher.onApplicationCreate(this)

    // periodic backup sync
    SyncWorker.schedulePeriodicSync(applicationContext)

    // start persistent network-listener foreground service once
    val svc = Intent(this, PersistentSyncService::class.java)
    startForegroundService(svc)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }
}

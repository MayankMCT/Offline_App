# 🔧 Background Sync Troubleshooting Guide

Common issues and how to fix them.

---

## 🔍 How to Debug

### **View Logs**

**Option 1: ADB Logcat (Recommended)**
```bash
# View all logs
adb logcat

# Filter for your app only
adb logcat | grep "com.yourapp"

# Filter for sync-related logs
adb logcat | grep -E "SyncWorker|HeadlessJS|BackgroundSync"
```

**Option 2: Android Studio**
- Open Logcat tab at bottom
- Select your device
- Filter by "SyncWorker"

### **What to Look For:**

✅ **Good logs:**
```
SyncWorker: WorkManager task started
SyncWorker: HeadlessJsTaskService started
HeadlessJS: --- HeadlessJS Sync Task Start ---
HeadlessJS: Syncing 3 tasks...
HeadlessJS: ✅ Synced task: Task 1
HeadlessJS: --- HeadlessJS Sync Task Success ---
```

❌ **Problem logs:**
```
SyncWorker: HeadlessJsTaskService failed to start
HeadlessJS: timeout error
WorkManager: constraints not met
```

---

## 🐛 Issue 1: No Notification Appears

### **Symptoms:**
- Internet comes back
- No notification shows
- Sync doesn't happen

### **Causes & Fixes:**

#### **Cause 1: App doesn't have notification permission**

**Check:**
- Phone Settings → Apps → Your App → Notifications
- Ensure "All notifications" is enabled

**Fix:**
```kotlin
// Request permission in your app (for Android 13+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
}
```

#### **Cause 2: Notification channel not created**

**Check logs for:**
```
SyncWorker: createNotificationChannel() called
```

**Fix:**
- Verify `createNotificationChannel()` is called in SyncWorker
- Increase notification importance:
```java
NotificationManager.IMPORTANCE_LOW  // Change to
NotificationManager.IMPORTANCE_DEFAULT
```

#### **Cause 3: Battery optimization is killing the service**

**Fix:**
- Phone Settings → Apps → Your App → Battery
- Select "Unrestricted" or "Don't optimize"

**Test command:**
```bash
# Disable battery optimization for testing
adb shell dumpsys deviceidle whitelist +com.yourapp.package
```

---

## 🐛 Issue 2: Sync Doesn't Work When App is Killed

### **Symptoms:**
- Kill app from recent apps
- Internet comes back
- Nothing happens

### **Causes & Fixes:**

#### **Cause 1: PersistentSyncService not running**

**Check logs:**
```bash
adb logcat | grep "PersistentSyncService"
```

**Look for:**
```
PersistentSyncService: onCreate
PersistentSyncService: NETWORK AVAILABLE!
```

**If not found, fix:**

1. Verify service is registered in AndroidManifest.xml:
```xml
<service
    android:name=".PersistentSyncService"
    android:exported="false" />
```

2. Start the service in MainApplication.kt:
```kotlin
override fun onCreate() {
    super.onCreate()
    // Start persistent service
    val intent = Intent(this, PersistentSyncService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}
```

#### **Cause 2: WorkManager not scheduled**

**Check:**
```bash
adb shell dumpsys jobscheduler | grep "com.yourapp"
```

**Look for:** Your SyncWorker job scheduled

**Fix:**
- Verify `SyncWorker.schedulePeriodicSync()` is called in MainApplication.onCreate()
- Check WorkManager dependency is added in build.gradle

#### **Cause 3: Phone manufacturer aggressive battery optimization**

**Affected brands:**
- Xiaomi (MIUI)
- Huawei (EMUI)
- Oppo/Realme (ColorOS)
- Vivo (FuntouchOS)
- OnePlus (OxygenOS)

**Fix:**
- **Xiaomi:** Settings → Battery → App battery saver → Your App → No restrictions
- **Huawei:** Settings → Battery → Launch → Your App → Manage manually (enable all)
- **Oppo:** Settings → Battery → App Quick Freeze → Disable for your app
- **OnePlus:** Settings → Battery → Battery Optimization → Your App → Don't optimize

**Test site:** https://dontkillmyapp.com/

---

## 🐛 Issue 3: HeadlessJS Timeout Error

### **Symptoms:**
```
HeadlessJS: Task timed out
WorkManager: Result.failure()
```

### **Causes & Fixes:**

#### **Cause 1: Sync takes too long**

**Default timeout:** 5 seconds

**Fix:** Increase timeout in `SyncHeadlessTaskService.java`:
```java
return new HeadlessJsTaskConfig(
    "BackgroundSync",
    Arguments.createMap(),
    10000, // Increased to 10 seconds
    true
);
```

#### **Cause 2: Network is slow**

**Fix:** Add timeout to your API calls:
```typescript
const response = await fetch(API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
    signal: AbortSignal.timeout(8000) // 8 second timeout
});
```

#### **Cause 3: Syncing too much data at once**

**Fix:** Add pagination:
```typescript
export async function syncPendingChanges(): Promise<boolean> {
    const tasksToSync = await getTasks({
        syncStatus: 'pending',
        limit: 10 // Only sync 10 at a time
    });
    // ... sync logic
}
```

---

## 🐛 Issue 4: Sync Stops After Phone Reboot

### **Symptoms:**
- Sync works initially
- Restart phone
- Sync never runs again

### **Causes & Fixes:**

#### **Cause 1: BootCompletedReceiver not registered**

**Fix:** Add to AndroidManifest.xml:
```xml
<receiver
    android:name=".BootCompletedReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

#### **Cause 2: Missing BOOT_COMPLETED permission**

**Fix:** Add to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

#### **Cause 3: Receiver not triggering**

**Test manually:**
```bash
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
```

**Check logs:**
```bash
adb logcat | grep "BootCompletedReceiver"
```

**Should see:**
```
BootCompletedReceiver: Phone rebooted. Re-scheduling periodic sync.
```

---

## 🐛 Issue 5: Build Errors

### **Error: "Cannot resolve symbol WorkManager"**

**Fix:**
```groovy
// Add to android/app/build.gradle
dependencies {
    implementation("androidx.work:work-runtime:2.8.1")
}
```

Then sync Gradle files and rebuild.

### **Error: "Package com.testoffline.sync does not exist"**

**Fix:** Update package name in all Java files:
```java
package com.testoffline.sync; // Change this to your package
```

### **Error: "Task 'BackgroundSync' not registered"**

**Fix:** Verify:
1. `headlessTask.js` exists and has:
```javascript
AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);
```

2. `index.js` imports it:
```javascript
import './headlessTask';
```

3. Clean and rebuild:
```bash
cd android
./gradlew clean
cd ..
npx expo run:android
```

### **Error: "Execution failed for task ':app:mergeDebugResources'"**

**Fix:** Clean build:
```bash
cd android
./gradlew clean
./gradlew cleanBuildCache
cd ..
rm -rf android/app/build
npx expo run:android
```

---

## 🐛 Issue 6: Sync Works Only When App is Open

### **Symptoms:**
- Sync works when app is in foreground
- Stops working when app is killed

### **This means:**
You're using JavaScript timers instead of native background tasks.

### **Fix:**
Don't use these (they stop when app is killed):
```javascript
// ❌ These don't work when app is killed
setInterval(() => sync(), 60000);
setTimeout(() => sync(), 60000);
BackgroundTimer.setInterval(() => sync(), 60000);
```

Use native WorkManager instead:
```java
// ✅ This works when app is killed
SyncWorker.schedulePeriodicSync(context);
```

---

## 🐛 Issue 7: Multiple Notifications Stacking Up

### **Symptoms:**
- Every sync creates a new notification
- Multiple notifications visible

### **Fix:**
Use same notification ID in SyncWorker.java:
```java
private static final int NOTIFICATION_ID = 12345; // Same ID

// When updating notification:
notificationManager.notify(NOTIFICATION_ID, notification.build()); // Reuses same notification
```

---

## 🐛 Issue 8: Sync Happens Too Frequently

### **Symptoms:**
- Notification appears every few seconds
- Battery drains quickly

### **Causes & Fixes:**

#### **Cause 1: Multiple triggers**

**Check if you're calling sync from:**
- PersistentSyncService ✅ (instant)
- WorkManager ✅ (periodic)
- JavaScript timers ❌ (remove these)
- Network listeners in JS ❌ (remove these)

**Fix:** Only use native triggers, remove JS sync triggers when app is in background.

#### **Cause 2: Network fluctuating**

**Fix:** Add debouncing in PersistentSyncService:
```java
private long lastSyncTime = 0;
private static final long MIN_SYNC_INTERVAL = 60000; // 1 minute

@Override
public void onAvailable(@NonNull Network network) {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastSyncTime > MIN_SYNC_INTERVAL) {
        lastSyncTime = currentTime;
        SyncWorker.scheduleOneTimeSync(getApplicationContext());
    }
}
```

---

## 🧪 Testing Checklist

### **Test 1: Basic Sync**
```bash
# 1. Turn off WiFi
# 2. Add a task in app
# 3. Kill app
# 4. Turn on WiFi
# 5. Check notification appears
# Expected: ✅ Notification shows, task syncs
```

### **Test 2: Airplane Mode**
```bash
# 1. Enable airplane mode
# 2. Add a task
# 3. Kill app
# 4. Disable airplane mode
# Expected: ✅ Sync triggers
```

### **Test 3: Phone Reboot**
```bash
# 1. Add pending task
# 2. Restart phone (don't open app)
# 3. Wait 15 minutes
# Expected: ✅ Periodic sync resumes
```

### **Test 4: Battery Saver**
```bash
# 1. Enable battery saver
# 2. Repeat Test 1
# Expected: ✅ Sync works (may be delayed)
```

### **Test 5: Long Offline Period**
```bash
# 1. Stay offline for 1+ hour
# 2. Add multiple tasks
# 3. Kill app
# 4. Go online
# Expected: ✅ All tasks sync
```

---

## 📊 Performance Monitoring

### **Battery Usage**
```bash
# Check battery usage
adb shell dumpsys batterystats --reset
# Use app for a day
adb shell dumpsys batterystats | grep "com.yourapp"
```

**Good:** < 5% battery per day
**Acceptable:** 5-10% battery per day
**Too high:** > 10% battery per day (optimize!)

### **WorkManager Status**
```bash
# Check scheduled jobs
adb shell dumpsys jobscheduler | grep "com.yourapp"
```

**Look for:**
- Job scheduled: Yes ✅
- Constraints met: Yes ✅
- Last run: Recent timestamp ✅

### **Memory Usage**
```bash
# Check memory
adb shell dumpsys meminfo com.yourapp.package
```

**Persistent service should use:** < 10 MB RAM

---

## 🔧 Advanced Debugging

### **Enable WorkManager Debug Logging**

Add to `MainApplication.kt`:
```kotlin
import androidx.work.Configuration

class MainApplication : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
```

### **Test WorkManager Manually**
```bash
# Force run a worker
adb shell cmd jobscheduler run -f com.yourapp.package <job-id>
```

### **Check Network Constraints**
```bash
# Verify network is available
adb shell dumpsys connectivity
```

---

## 🆘 Still Not Working?

### **Step 1: Verify Setup**
- [ ] All 6 Java files created in correct package
- [ ] All files have correct package name
- [ ] AndroidManifest.xml has all permissions and receivers
- [ ] build.gradle has WorkManager dependency
- [ ] MainApplication schedules periodic sync
- [ ] headlessTask.js is imported in index.js

### **Step 2: Clean Everything**
```bash
# Kill app
adb shell am force-stop com.yourapp.package

# Clear app data
adb shell pm clear com.yourapp.package

# Uninstall
adb uninstall com.yourapp.package

# Clean build
cd android
./gradlew clean
cd ..
rm -rf android/app/build

# Rebuild
npx expo run:android
```

### **Step 3: Test Simple Case**
Create minimal test in `syncService.ts`:
```typescript
export async function fullSync() {
    console.log('=== TEST SYNC CALLED ===');
    return Promise.resolve();
}
```

If this shows in logs when internet returns → Native setup is correct ✅
If not → Check native code registration

### **Step 4: Check Sample Implementation**
Compare your files with working implementation in this repo:
- `d:\new working without modules\android\app\src\main\java\com\testoffline\sync\`

---

## 📞 Getting Help

### **Information to Provide:**

1. **Device Info:**
   - Brand/Model: (e.g., Samsung Galaxy S21)
   - Android Version: (e.g., Android 13)
   - ROM: (e.g., Stock, MIUI, ColorOS)

2. **Logs:**
```bash
adb logcat > logs.txt
# Share logs.txt
```

3. **What You Tried:**
   - List troubleshooting steps already attempted
   - Include code changes made

4. **Expected vs Actual:**
   - What should happen
   - What actually happens

---

## 💡 Pro Tips

1. **Always test on real device** - Emulators behave differently
2. **Test on multiple brands** - Samsung, Xiaomi, Huawei behave differently
3. **Start simple** - Get basic sync working before adding features
4. **Use logs liberally** - Add console.log everywhere while debugging
5. **Test with slow network** - Use Chrome DevTools to throttle
6. **Monitor battery** - Optimize if usage is too high
7. **Handle errors gracefully** - Add try-catch blocks everywhere
8. **Version control** - Commit working version before making changes

---

## ✅ Success Indicators

You know it's working when:

- ✅ Kill app → Turn on internet → Notification appears within 10 seconds
- ✅ Logs show "HeadlessJS Sync Task Success"
- ✅ Data appears in your server/API
- ✅ Works after phone reboot
- ✅ Battery usage is reasonable (< 5% per day)
- ✅ Works on different phone brands
- ✅ Works with battery saver enabled (may be delayed)

---

**Happy Debugging! 🐛🔧**

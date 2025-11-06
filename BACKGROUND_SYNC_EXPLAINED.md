# 📱 Background Sync Implementation - Complete Guide

## 🎯 What Does This Do?

Your app can now **sync data even when it's completely killed** (removed from recent apps). When your phone gets internet connection, the app automatically syncs pending changes **WITHOUT needing to be opened**.

---

## 🧠 The Big Picture: How It Works

Think of it like having **3 backup plans** to ensure syncing never fails:

1. **📞 Instant Listener** - Listens for internet connection 24/7 (even when app is killed)
2. **⏰ Periodic Backup** - Syncs every 15 minutes as a safety net
3. **🔄 Reboot Protection** - Re-enables everything when phone restarts

---

## 📦 Main Components

### **1. SyncWorker.java** - The Sync Executor
**Location:** `android/app/src/main/java/com/testoffline/sync/SyncWorker.java`

**What it does:**
- This is the **brain** that actually performs the sync
- Shows a notification when syncing starts
- Runs your JavaScript sync code without opening the app

**Key Parts:**
```java
public Result doWork() {
    // 1. Show notification "Starting sync..."
    createNotificationChannel();
    ForegroundInfo foregroundInfo = createForegroundInfo("Starting background sync...");
    setForegroundAsync(foregroundInfo);

    // 2. Start the JavaScript sync
    Intent service = new Intent(context, SyncHeadlessTaskService.class);
    context.startService(service);

    // 3. Update notification to "Sync in progress..."
    updateNotification("Sync in progress...");
}
```

**Two ways to trigger it:**
- `schedulePeriodicSync()` - Syncs every 15 minutes automatically
- `scheduleOneTimeSync()` - Syncs immediately when internet connects

---

### **2. PersistentSyncService.java** - The 24/7 Listener
**Location:** `android/app/src/main/java/com/testoffline/sync/PersistentSyncService.java`

**What it does:**
- Runs **24/7 in the background** even when app is killed
- **Instantly detects** when internet comes back
- Shows a persistent notification (required by Android)

**Key Parts:**
```java
networkCallback = new ConnectivityManager.NetworkCallback() {
    @Override
    public void onAvailable(@NonNull Network network) {
        Log.i(TAG, "NETWORK AVAILABLE! Triggering one-time sync.");
        // Internet is back! Tell SyncWorker to sync NOW
        SyncWorker.scheduleOneTimeSync(getApplicationContext());
    }
};
```

**Why it exists:**
- Android **kills background processes** when app is closed
- A **Foreground Service** with notification keeps it alive
- This is the **ONLY way** to listen for internet when app is killed

---

### **3. SyncHeadlessTaskService.java** - The JavaScript Runner
**Location:** `android/app/src/main/java/com/testoffline/sync/SyncHeadlessTaskService.java`

**What it does:**
- Runs JavaScript code **without opening the app**
- Connects native Android code to your React Native sync logic

**Key Parts:**
```java
protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
    return new HeadlessJsTaskConfig(
        "BackgroundSync", // JavaScript task name
        Arguments.createMap(),
        5000, // 5 second timeout
        true
    );
}
```

---

### **4. headlessTask.js** - The JavaScript Entry Point
**Location:** `headlessTask.js`

**What it does:**
- This is the JavaScript code that runs in the background
- Calls your actual sync logic (`fullSync()`)

**Code:**
```javascript
import { AppRegistry } from 'react-native';
import { fullSync } from './services/syncService';

const BackgroundSync = async (taskData) => {
    console.log('--- HeadlessJS Sync Task Start ---');
    try {
        await fullSync(); // Your sync logic
        console.log('--- HeadlessJS Sync Task Success ---');
    } catch (error) {
        console.error('--- HeadlessJS Sync Task Error ---', error);
    }
};

// Register this task with name "BackgroundSync"
AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);
```

**Connected in:** `index.js`
```javascript
import "expo-router/entry";
import './headlessTask'; // This registers the background task
```

---

### **5. BootCompletedReceiver.java** - The Reboot Guard
**Location:** `android/app/src/main/java/com/testoffline/sync/BootCompletedReceiver.java`

**What it does:**
- When phone **reboots**, all scheduled tasks are canceled
- This receiver **re-schedules** the 15-minute periodic sync

**Code:**
```java
public void onReceive(Context context, Intent intent) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
        Log.d("BootCompletedReceiver", "Phone rebooted. Re-scheduling periodic sync.");
        SyncWorker.schedulePeriodicSync(context); // Reschedule!
    }
}
```

---

### **6. MainApplication.kt** - The Startup Initializer
**Location:** `android/app/src/main/java/com/testoffline/sync/MainApplication.kt`

**What it does:**
- Runs when app **first opens**
- Schedules the 15-minute periodic sync
- Registers custom native modules

**Key Parts:**
```kotlin
override fun onCreate() {
    super.onCreate()
    // ... other initialization

    // Schedule the periodic sync when app first opens
    SyncWorker.schedulePeriodicSync(applicationContext)
}

override fun getPackages(): List<ReactPackage> {
    val packages = PackageList(this).packages.toMutableList()
    packages.add(BackgroundSyncPackage()) // Register custom module
    return packages
}
```

---

### **7. BackgroundSyncModule.java + BackgroundSyncPackage.java**
**Location:** `android/app/src/main/java/com/testoffline/sync/`

**What it does:**
- Creates a bridge between JavaScript and native Android
- Allows you to call native functions from React Native

**Purpose:**
- You can manually trigger sync from JS if needed
- Provides `initialize()` and `triggerSync()` methods

---

### **8. syncService.ts** - Your Actual Sync Logic
**Location:** `services/syncService.ts`

**What it does:**
- Contains the **actual business logic** for syncing
- Uploads pending tasks to server
- Fetches updates from server

**Key Functions:**
```typescript
// Send pending local changes to server
export async function syncPendingChanges(): Promise<boolean> {
    const tasksToSync = pendingTasks.filter(t => t.syncStatus === 'pending');
    // Upload each task to your API
    // Mark as 'synced' in local database
}

// Fetch updates from server
export async function fetchServerUpdates(): Promise<void> {
    // Get latest data from your API
    // Update local database
}

// Do both!
export async function fullSync(): Promise<void> {
    await syncPendingChanges();
    await fetchServerUpdates();
}
```

---

## 🔗 How Everything Connects

### **Flow 1: When Internet Comes Back (App Killed)**

```
📡 Internet Connected
    ↓
🎧 PersistentSyncService detects it (running 24/7)
    ↓
⚙️ Calls SyncWorker.scheduleOneTimeSync()
    ↓
📲 SyncWorker shows notification
    ↓
🚀 Starts SyncHeadlessTaskService
    ↓
📄 Runs headlessTask.js → fullSync()
    ↓
✅ Data synced!
```

### **Flow 2: Periodic Backup (Every 15 Minutes)**

```
⏰ 15 minutes pass
    ↓
⚙️ WorkManager triggers SyncWorker
    ↓
📲 SyncWorker shows notification
    ↓
🚀 Starts SyncHeadlessTaskService
    ↓
📄 Runs headlessTask.js → fullSync()
    ↓
✅ Data synced!
```

### **Flow 3: After Phone Reboot**

```
🔄 Phone restarts
    ↓
📡 BootCompletedReceiver wakes up
    ↓
⚙️ Calls SyncWorker.schedulePeriodicSync()
    ↓
✅ 15-minute sync is re-enabled!
```

---

## 📋 Required Configuration Files

### **AndroidManifest.xml**
**Location:** `android/app/src/main/AndroidManifest.xml`

**Permissions Added:**
```xml
<!-- Internet and network detection -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- For foreground services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>

<!-- For listening to boot events -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

<!-- For background tasks -->
<uses-permission android:name="android.permission.WAKE_LOCK"/>
```

**Services and Receivers Registered:**
```xml
<!-- The headless task runner -->
<service
    android:name=".SyncHeadlessTaskService"
    android:exported="false" />

<!-- Listens for boot events to reschedule sync -->
<receiver
    android:name=".BootCompletedReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

### **build.gradle**
**Location:** `android/app/build.gradle`

**Dependency Added:**
```groovy
dependencies {
    // WorkManager for background sync when app is killed
    implementation("androidx.work:work-runtime:2.8.1")
}
```

---

## 🎓 Key Concepts to Understand

### **1. WorkManager**
- Android's **official** solution for reliable background work
- Survives app kills, device reboots, and battery optimization
- Handles scheduling, retries, and constraints (like "only when internet is available")

### **2. Foreground Service**
- A service that shows a **persistent notification**
- Android doesn't kill it (because user can see it's running)
- Required for **24/7 network listening**

### **3. Headless JS**
- React Native feature that runs JavaScript **without UI**
- Allows your sync logic to run when app is killed
- Native code starts it, JavaScript executes

### **4. BroadcastReceiver**
- Listens for system events (like boot completed)
- Can wake up even when app is completely dead
- Used to reschedule tasks after reboot

---

## 🚀 How to Implement This in Another App

### **Step 1: Copy These Files**
1. `SyncWorker.java` - The sync executor
2. `PersistentSyncService.java` - The 24/7 listener
3. `SyncHeadlessTaskService.java` - The JS runner
4. `BootCompletedReceiver.java` - The reboot guard
5. `BackgroundSyncModule.java` + `BackgroundSyncPackage.java` - The JS bridge
6. `headlessTask.js` - The JS entry point

### **Step 2: Update AndroidManifest.xml**
- Add all the permissions
- Register the service and receivers

### **Step 3: Update build.gradle**
- Add WorkManager dependency

### **Step 4: Update MainApplication.kt**
- Register `BackgroundSyncPackage()`
- Call `SyncWorker.schedulePeriodicSync()` in `onCreate()`

### **Step 5: Update index.js**
- Import `./headlessTask`

### **Step 6: Write Your Sync Logic**
- Create your own `syncService.ts`
- Implement `fullSync()` function
- Call your API to upload/download data

### **Step 7: Adjust Settings**
- Change sync interval (15 minutes → your preference)
- Change notification text and icon
- Adjust timeout values if needed

---

## 🔧 Important Settings You Can Change

### **Sync Interval**
In `SyncWorker.java`:
```java
PeriodicWorkRequest syncWorkRequest =
    new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES) // Change 15
```

### **Notification Text**
In `SyncWorker.java`:
```java
.setContentTitle("Offline App") // Your app name
.setContentText("Starting background sync...") // Your message
```

### **Timeout**
In `SyncHeadlessTaskService.java`:
```java
return new HeadlessJsTaskConfig(
    "BackgroundSync",
    Arguments.createMap(),
    5000, // 5 seconds - increase if sync takes longer
    true
);
```

---

## ⚠️ Common Issues & Solutions

### **Issue 1: Notification doesn't appear**
**Solution:** Check if app has notification permission enabled in phone settings

### **Issue 2: Sync doesn't work when app is killed**
**Solution:**
- Check battery optimization is OFF for your app
- Verify PersistentSyncService is registered in AndroidManifest
- Check WorkManager dependency is added

### **Issue 3: Sync stops after phone reboot**
**Solution:** Verify BootCompletedReceiver is registered and has BOOT_COMPLETED permission

### **Issue 4: HeadlessJS timeout**
**Solution:** Increase timeout in `SyncHeadlessTaskService.java` from 5000 to 10000 or more

---

## 🎯 Testing Checklist

1. ✅ **Test with app killed:**
   - Go offline
   - Add a task (should show "pending")
   - Kill app from recent apps
   - Go online
   - Check notification appears
   - Open app and verify task is "synced"

2. ✅ **Test periodic sync:**
   - Wait 15 minutes
   - Check if sync happens automatically

3. ✅ **Test after reboot:**
   - Restart phone
   - Wait 15 minutes
   - Verify sync still happens

4. ✅ **Test with battery saver:**
   - Enable battery saver mode
   - Test if sync still works

---

## 💡 Why This Architecture?

### **Why not just JavaScript?**
- JavaScript **CANNOT run** when app is killed
- Need native Android code to stay alive

### **Why WorkManager?**
- It's **battery efficient**
- Handles retries automatically
- Survives app kills and reboots
- Official Google recommendation

### **Why Foreground Service?**
- Only way to listen for events 24/7
- Android won't kill it (because of notification)
- Provides instant sync when internet returns

### **Why Three Components?**
- **PersistentSyncService** - For instant sync (when internet comes back)
- **WorkManager** - For reliable periodic sync (every 15 min)
- **BootCompletedReceiver** - For reboot protection

**Result:** Triple redundancy ensures sync NEVER fails! 🎉

---

## 📝 Summary

This background sync system works by:

1. **Running a foreground service** (PersistentSyncService) that listens for internet 24/7
2. **Using WorkManager** to schedule periodic syncs as backup
3. **Running JavaScript without UI** (Headless JS) to execute sync logic
4. **Showing notifications** to keep user informed
5. **Re-scheduling after reboot** to ensure nothing breaks

The beauty is that **even if one system fails, the others ensure sync happens**. It's bulletproof! 🛡️

---

## 🎓 Key Takeaway for Other Apps

To implement background sync in ANY React Native app:

1. **Native Side:** Use WorkManager + Foreground Service + BroadcastReceiver
2. **Bridge:** Use Headless JS to run your JavaScript sync code
3. **JavaScript Side:** Write your sync logic that talks to your API
4. **Configuration:** Register everything in AndroidManifest and build.gradle

This is the **ONLY reliable way** to sync when app is killed on Android! 🚀

---

**Questions? Check the code comments in each file for more details!**

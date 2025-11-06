# 🚀 Quick Implementation Checklist

Use this checklist to implement background sync in any React Native app.

---

## ✅ Step-by-Step Implementation

### **Phase 1: Setup Native Android Files**

- [ ] **Create Java package folder**
  - Path: `android/app/src/main/java/com/yourapp/sync/`

- [ ] **Copy these 6 Java files:**
  - [ ] `SyncWorker.java` - The sync executor
  - [ ] `PersistentSyncService.java` - The 24/7 listener
  - [ ] `SyncHeadlessTaskService.java` - The JavaScript runner
  - [ ] `BootCompletedReceiver.java` - The reboot guard
  - [ ] `BackgroundSyncModule.java` - The JavaScript bridge
  - [ ] `BackgroundSyncPackage.java` - The package registration

- [ ] **Update package names in all files**
  - Replace `com.testoffline.sync` with your app's package name

---

### **Phase 2: Update Configuration Files**

- [ ] **AndroidManifest.xml** (`android/app/src/main/AndroidManifest.xml`)

  Add permissions:
  ```xml
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
  <uses-permission android:name="android.permission.WAKE_LOCK"/>
  ```

  Add service and receivers inside `<application>` tag:
  ```xml
  <service
      android:name=".SyncHeadlessTaskService"
      android:exported="false" />

  <receiver
      android:name=".BootCompletedReceiver"
      android:enabled="true"
      android:exported="true">
      <intent-filter>
          <action android:name="android.intent.action.BOOT_COMPLETED"/>
      </intent-filter>
  </receiver>
  ```

- [ ] **build.gradle** (`android/app/build.gradle`)

  Add dependency in `dependencies` block:
  ```groovy
  implementation("androidx.work:work-runtime:2.8.1")
  ```

- [ ] **MainApplication.kt** (or `.java`)

  Import and register:
  ```kotlin
  import com.yourapp.sync.SyncWorker
  import com.yourapp.sync.BackgroundSyncPackage

  // In getPackages():
  packages.add(BackgroundSyncPackage())

  // In onCreate():
  SyncWorker.schedulePeriodicSync(applicationContext)
  ```

---

### **Phase 3: Setup JavaScript Files**

- [ ] **Create headlessTask.js** (in root folder)
  ```javascript
  import { AppRegistry } from 'react-native';
  import { fullSync } from './services/syncService'; // Your sync logic

  const BackgroundSync = async (taskData) => {
      console.log('--- HeadlessJS Sync Task Start ---');
      try {
          await fullSync();
          console.log('--- HeadlessJS Sync Task Success ---');
      } catch (error) {
          console.error('--- HeadlessJS Sync Task Error ---', error);
      }
  };

  AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);
  ```

- [ ] **Update index.js** (in root folder)
  ```javascript
  import "expo-router/entry"; // or your entry point
  import './headlessTask'; // Add this line
  ```

- [ ] **Create/Update syncService.ts**
  - [ ] Implement `syncPendingChanges()` - Upload local changes to server
  - [ ] Implement `fetchServerUpdates()` - Download updates from server
  - [ ] Implement `fullSync()` - Do both operations
  - [ ] Use your actual API endpoints and database logic

---

### **Phase 4: Customize Settings**

- [ ] **Change sync interval** (default is 15 minutes)
  - File: `SyncWorker.java`
  - Line: `new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)`
  - Change `15` to your preferred interval

- [ ] **Change notification text**
  - File: `SyncWorker.java`
  - Update `.setContentTitle()` and `.setContentText()` methods

- [ ] **Change app icon in notification**
  - File: `SyncWorker.java`
  - Line: `.setSmallIcon(R.mipmap.ic_launcher)`
  - Use your custom icon resource

- [ ] **Adjust timeout** (if sync takes longer than 5 seconds)
  - File: `SyncHeadlessTaskService.java`
  - Line: `5000` (milliseconds)
  - Increase if needed (e.g., `10000` for 10 seconds)

---

### **Phase 5: Build and Test**

- [ ] **Clean and rebuild**
  ```bash
  cd android
  ./gradlew clean
  cd ..
  npx expo run:android
  ```
  Or PowerShell:
  ```powershell
  cd android
  .\gradlew.bat clean
  cd ..
  npx expo run:android
  ```

- [ ] **Test Scenario 1: App Killed**
  1. Turn off WiFi and mobile data
  2. Open app and add a task (should show "pending")
  3. Kill app from recent apps
  4. Turn on WiFi
  5. Check notification appears
  6. Open app and verify task is "synced"

- [ ] **Test Scenario 2: Periodic Sync**
  1. Wait for your sync interval (e.g., 15 minutes)
  2. Check if notification appears
  3. Verify data synced

- [ ] **Test Scenario 3: After Reboot**
  1. Add a pending task
  2. Restart phone
  3. Wait for sync interval
  4. Verify sync still works

- [ ] **Test Scenario 4: Battery Saver**
  1. Enable battery saver mode
  2. Repeat Test Scenario 1
  3. Verify sync still works (may be delayed)

---

## 🔧 Common Issues & Solutions

### **Issue: Notification doesn't appear**

- [ ] Check app has notification permission in phone settings
- [ ] Verify `FOREGROUND_SERVICE` permission is in AndroidManifest
- [ ] Check notification channel is created (Android 8+)
- [ ] Increase notification importance from `LOW` to `DEFAULT`

### **Issue: Sync doesn't work when app is killed**

- [ ] Disable battery optimization for your app
- [ ] Verify `PersistentSyncService` is registered in AndroidManifest
- [ ] Check WorkManager dependency is added in build.gradle
- [ ] Verify `schedulePeriodicSync()` is called in MainApplication.onCreate()

### **Issue: Sync stops after phone reboot**

- [ ] Verify `BootCompletedReceiver` is registered in AndroidManifest
- [ ] Check `RECEIVE_BOOT_COMPLETED` permission is added
- [ ] Test with `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`

### **Issue: HeadlessJS timeout error**

- [ ] Increase timeout in `SyncHeadlessTaskService.java` (from 5000 to 10000+)
- [ ] Optimize your sync logic to be faster
- [ ] Add pagination if syncing large datasets

### **Issue: Build errors**

- [ ] Check all package names match your app
- [ ] Verify all imports are correct
- [ ] Run `./gradlew clean` and rebuild
- [ ] Check Gradle version compatibility

### **Issue: WorkManager not found**

- [ ] Verify dependency is added: `implementation("androidx.work:work-runtime:2.8.1")`
- [ ] Sync Gradle files
- [ ] Clean and rebuild

---

## 📝 File Locations Summary

```
your-react-native-app/
│
├── index.js                              ← Import headlessTask
├── headlessTask.js                       ← Create this file
│
├── services/
│   └── syncService.ts                    ← Your sync logic
│
└── android/
    ├── app/
    │   ├── build.gradle                  ← Add WorkManager dependency
    │   └── src/
    │       └── main/
    │           ├── AndroidManifest.xml   ← Add permissions & receivers
    │           └── java/
    │               └── com/
    │                   └── yourapp/
    │                       └── sync/     ← Create this folder
    │                           ├── SyncWorker.java
    │                           ├── PersistentSyncService.java
    │                           ├── SyncHeadlessTaskService.java
    │                           ├── BootCompletedReceiver.java
    │                           ├── BackgroundSyncModule.java
    │                           ├── BackgroundSyncPackage.java
    │                           └── MainApplication.kt (update this)
```

---

## 🎯 What Each File Does (Quick Reference)

| File | Purpose | When it runs |
|------|---------|--------------|
| `SyncWorker.java` | Executes sync, shows notification | When WorkManager triggers |
| `PersistentSyncService.java` | Listens for internet 24/7 | Always (even when app killed) |
| `SyncHeadlessTaskService.java` | Runs JavaScript without UI | When SyncWorker calls it |
| `BootCompletedReceiver.java` | Re-schedules sync after reboot | When phone starts |
| `BackgroundSyncModule.java` | Bridges native ↔ JavaScript | When JavaScript calls native |
| `BackgroundSyncPackage.java` | Registers the module | App startup |
| `headlessTask.js` | JavaScript entry point | When native calls it |
| `syncService.ts` | Your actual sync logic | When headlessTask calls it |

---

## 💡 Pro Tips

1. **Start simple:** Get basic sync working first, then add optimizations
2. **Test on real device:** Emulators may not accurately simulate background behavior
3. **Check logs:** Use `adb logcat` to see background sync logs
4. **Monitor battery:** Background services consume battery - optimize your sync logic
5. **Handle errors:** Add try-catch blocks and retry logic in your sync service
6. **Version compatibility:** Test on Android 8+ (where background restrictions are stricter)
7. **Debug notifications:** If you don't see them, check notification settings for your app
8. **Respect user data:** Only sync when on WiFi if handling large data (add network constraints)

---

## 📚 Additional Resources

- **WorkManager Docs:** https://developer.android.com/topic/libraries/architecture/workmanager
- **Headless JS Docs:** https://reactnative.dev/docs/headless-js-android
- **Background Tasks Guide:** https://developer.android.com/guide/background

---

## ✨ Success Criteria

Your implementation is complete when:

- ✅ App syncs when killed and internet returns
- ✅ Notification appears during sync
- ✅ Periodic sync runs every X minutes
- ✅ Sync works after phone reboot
- ✅ No crashes or errors in logs
- ✅ Battery usage is reasonable
- ✅ User data is preserved offline

---

**Happy Coding! 🚀**

*For detailed explanation, see `BACKGROUND_SYNC_EXPLAINED.md`*
*For visual diagrams, see `SYNC_ARCHITECTURE_DIAGRAM.md`*

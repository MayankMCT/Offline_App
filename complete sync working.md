# Complete Background Sync Working in React Native (Android)

This document explains how background synchronization is implemented in this React Native application, focusing on how it works even when the app is killed or in the background. It also provides a guide on how to implement similar functionality in your own React Native projects.

## Overview

This application uses a combination of React Native's Headless JS tasks, Android Foreground Services, WorkManager, and a local database (WatermelonDB) to achieve robust background data synchronization. This setup ensures that data can be synced to and from a remote server even when the app is not actively in use, providing an offline-first experience.

**Key Components:**

1.  **WatermelonDB**: A local database for storing application data, enabling offline access and tracking changes.
2.  **`syncService.ts`**: Contains the core logic for pushing local changes to the server and pulling server updates to the local database.
3.  **`headlessTask.js`**: A React Native Headless JS task that executes the `syncService.ts` logic in the background.
4.  **Android Native Modules (`BackgroundSyncModule.java`)**: Bridges JavaScript with native Android features like managing persistent notifications and requesting battery optimization exemptions.
5.  **Android WorkManager (`SyncWorker.java`)**: Android's recommended API for scheduling deferrable, guaranteed background tasks. It triggers the Headless JS task.
6.  **Android Foreground Service (`PersistentSyncService.java`)**: A long-running service that displays a persistent notification, making it less likely for Android to kill the process. It monitors network connectivity and triggers immediate syncs when the network becomes available.
7.  **Android Broadcast Receiver (`BootCompletedReceiver.java`)**: Ensures that background services and periodic syncs are restarted after the device reboots.

## Detailed Explanation of Files

### JavaScript/TypeScript Files

*   **`headlessTask.js`**
    *   **Purpose**: This is the entry point for the JavaScript code that runs in the background. It's registered as a Headless JS task.
    *   **Why it's used**: React Native's mechanism to execute JavaScript code when the app is not in the foreground (e.g., killed, backgrounded). It orchestrates the actual data synchronization.
    *   **How it works**:
        *   It imports `fullSync` and `hasPendingChanges` from `syncService.ts`.
        *   It uses `NativeModules.BackgroundSyncModule` to interact with native Android code, specifically to update a persistent notification.
        *   It checks if there are pending changes using `hasPendingChanges()`.
        *   If changes exist, it updates the notification to "Sync in progress...", calls `fullSync()`, and then updates the notification to "Sync complete!" or "Sync failed."
        *   `AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);` registers this function to be called by the native side.

*   **`services/syncService.ts`**
    *   **Purpose**: Contains the business logic for synchronizing data between the local WatermelonDB and the remote API.
    *   **Why it's used**: Centralizes all sync-related operations, making them reusable and testable.
    *   **How it works**:
        *   `hasPendingChanges()`: Queries the local `tasks` collection in WatermelonDB to find records with `syncStatus === 'pending'`.
        *   `syncPendingChanges()`:
            *   Checks for internet connectivity using `@react-native-community/netinfo`.
            *   Fetches all pending tasks from WatermelonDB.
            *   Iterates through pending tasks, sends them to a `FAKE_API_URL` (your actual EAM API), and updates their `syncStatus` to `'synced'` in the local database upon successful upload.
        *   `fetchServerUpdates()`: (Currently a placeholder) Would fetch new data from the remote API and update the local database accordingly.
        *   `fullSync()`: Calls `syncPendingChanges()` first, then `fetchServerUpdates()` to perform a complete two-way sync.

*   **`database/database.ts`**
    *   **Purpose**: Initializes and configures the WatermelonDB database.
    *   **Why it's used**: Provides a robust, performant, and offline-first local database solution for React Native.
    *   **How it works**:
        *   Uses `SQLiteAdapter` to connect WatermelonDB to SQLite.
        *   Registers the database `schema` and `Task` model.
        *   Exports the `database` instance for use throughout the application.

*   **`database/models/Task.ts`**
    *   **Purpose**: Defines the WatermelonDB model for a `Task` entity.
    *   **Why it's used**: WatermelonDB models define the structure and behavior of data records.
    *   **How it works**: Specifies fields like `name`, `isCompleted`, and crucially, `syncStatus` (e.g., 'pending', 'synced') to track synchronization state.

*   **`database/schema.ts`**
    *   **Purpose**: Defines the database schema for WatermelonDB.
    *   **Why it's used**: WatermelonDB requires a schema definition to manage tables and columns.

*   **`index.js`**
    *   **Purpose**: The main entry file for the React Native application.
    *   **Why it's used**: It's where the application starts and where the Headless JS task is imported to ensure it's registered with `AppRegistry`.
    *   **How it works**: `import './headlessTask';` ensures that `headlessTask.js` is executed at app startup, registering the `BackgroundSync` task.

### Android Native Files (Java/Kotlin)

*   **`android/app/src/main/java/com/testoffline/sync/MainApplication.kt`**
    *   **Purpose**: The main entry point for the Android application.
    *   **Why it's used**: Initializes native modules and schedules background tasks when the app process starts.
    *   **How it works**:
        *   Registers `BackgroundSyncPackage` to expose native modules to JavaScript.
        *   In `onCreate()`:
            *   Calls `SyncWorker.schedulePeriodicSync()` to set up a recurring sync task (e.g., every 15 minutes).
            *   Starts `PersistentSyncService` as a foreground service using `startForegroundService()`, ensuring it runs continuously to monitor network changes.

*   **`android/app/src/main/java/com/testoffline/sync/BackgroundSyncPackage.java`**
    *   **Purpose**: A React Native package that registers native modules.
    *   **Why it's used**: It's the bridge that makes `BackgroundSyncModule`'s native methods available to the JavaScript side of the React Native application.
    *   **How it works**: Implements `ReactPackage` and returns a list containing an instance of `BackgroundSyncModule`.

*   **`android/app/src/main/java/com/testoffline/sync/BackgroundSyncModule.java`**
    *   **Purpose**: Provides native Android functionalities to the JavaScript code.
    *   **Why it's used**: Allows JavaScript to control Android-specific features like notifications and battery optimization settings.
    *   **How it works**:
        *   `@ReactMethod public void requestIgnoreBatteryOptimizations()`: Opens the Android settings to allow the user to exempt the app from battery optimizations, which is critical for reliable background execution.
        *   `@ReactMethod public void updateNotification()` and `resetNotification()`: Allows `headlessTask.js` to change the text and behavior (swipeable/non-swipeable) of the persistent notification displayed by `PersistentSyncService`.
        *   `@ReactMethod public void triggerSync()`: A utility method to manually trigger a one-time sync via `SyncWorker`.
        *   `public static void sendNetworkChangeEvent()`: A static method used by `PersistentSyncService` to emit events to the JavaScript side when network status changes.

*   **`android/app/src/main/java/com/testoffline/sync/BootCompletedReceiver.java`**
    *   **Purpose**: Ensures background services and periodic syncs are restarted after the device reboots.
    *   **Why it's used**: Android kills all app processes on reboot. This receiver ensures the background sync mechanism resumes automatically.
    *   **How it works**:
        *   Extends `BroadcastReceiver` and listens for `Intent.ACTION_BOOT_COMPLETED`.
        *   When a reboot is detected, it calls `SyncWorker.schedulePeriodicSync()` to re-schedule the periodic sync and starts `PersistentSyncService` again.

*   **`android/app/src/main/java/com/testoffline/sync/PersistentSyncService.java`**
    *   **Purpose**: A long-running Android Foreground Service that acts as a "watchman" for network changes.
    *   **Why it's used**: Foreground services are less likely to be killed by the Android system because they show a persistent notification, indicating to the user that the app is performing ongoing work. This is crucial for continuous background operation.
    *   **How it works**:
        *   Runs as a foreground service, displaying a notification (which can be updated by `BackgroundSyncModule`).
        *   Uses `ConnectivityManager.NetworkCallback` to register for network availability changes.
        *   When the network becomes `onAvailable()`, it logs the event, calls `BackgroundSyncModule.sendNetworkChangeEvent("online")` to notify JavaScript, and immediately schedules a one-time sync using `SyncWorker.scheduleOneTimeSync()`.
        *   When the network is `onLost()`, it notifies JavaScript.
        *   Returns `START_STICKY` in `onStartCommand` to tell Android to try and recreate the service if it gets killed.

*   **`android/app/src/main/java/com/testoffline/sync/SyncHeadlessTaskService.java`**
    *   **Purpose**: A native Android service that acts as a bridge to run the React Native Headless JS task.
    *   **Why it's used**: WorkManager (and other native components) can directly start this service, which then triggers the JavaScript `BackgroundSync` task.
    *   **How it works**:
        *   Extends `HeadlessJsTaskService` from React Native.
        *   `getTaskConfig()` specifies the name of the JavaScript task (`"BackgroundSync"`), a timeout, and allows it to run in the foreground.

*   **`android/app/src/main/java/com/testoffline/sync/SyncWorker.java`**
    *   **Purpose**: Utilizes Android's WorkManager to schedule and execute background sync tasks reliably.
    *   **Why it's used**: WorkManager handles deferring tasks, adding constraints (like network availability), and retrying failed tasks, making background operations robust and efficient.
    *   **How it works**:
        *   `doWork()`: This is the main method executed by WorkManager. It starts `SyncHeadlessTaskService`, which then runs the JavaScript `BackgroundSync` task. It also acquires a `WakeLock` to ensure the CPU stays awake during the task.
        *   `schedulePeriodicSync()`: Creates a `PeriodicWorkRequest` with network constraints (requires `CONNECTED` network) to run `SyncWorker` every 15 minutes. `ExistingPeriodicWorkPolicy.KEEP` ensures that if a periodic task is already scheduled, it's not duplicated.
        *   `scheduleOneTimeSync()`: Creates a `OneTimeWorkRequest` for immediate execution of `SyncWorker` when network is available. `ExistingWorkPolicy.REPLACE` ensures that if a one-time sync is already pending, it's replaced by the new request.

## Overall Flow of Background Sync

1.  **App Launch / Device Reboot**:
    *   When the app starts (`MainApplication.kt`) or the device reboots (`BootCompletedReceiver.java`), a periodic sync is scheduled via `SyncWorker.schedulePeriodicSync()`.
    *   The `PersistentSyncService` is started as a foreground service.
2.  **Continuous Monitoring (`PersistentSyncService`)**:
    *   The `PersistentSyncService` runs continuously, displaying a notification.
    *   It actively listens for network connectivity changes.
    *   If the network becomes available, it immediately triggers a one-time sync via `SyncWorker.scheduleOneTimeSync()`.
3.  **WorkManager Execution (`SyncWorker`)**:
    *   Whether triggered periodically or as a one-time event, WorkManager executes `SyncWorker.doWork()`.
    *   `doWork()` starts `SyncHeadlessTaskService`.
4.  **Headless JS Task (`SyncHeadlessTaskService` -> `headlessTask.js`)**:
    *   `SyncHeadlessTaskService` launches the `BackgroundSync` JavaScript task defined in `headlessTask.js`.
    *   `headlessTask.js` checks for pending changes in WatermelonDB.
    *   It updates the persistent notification (via `BackgroundSyncModule`) to "Sync in progress...".
    *   It calls `fullSync()` from `syncService.ts` to perform the actual data transfer.
    *   After `fullSync()` completes (successfully or with errors), it updates the notification to "Sync complete!" or "Sync failed." and makes it dismissible.
    *   After a short delay, it resets the notification to "Service is running in background".
5.  **Data Operations (`syncService.ts` & WatermelonDB)**:
    *   `syncService.ts` interacts with WatermelonDB to retrieve pending data and update synced data.
    *   It makes API calls to the remote server to push and pull data.

## How to Implement This in Another App

To implement this robust background sync functionality in another React Native Android app, follow these steps:

### Step 1: Set up WatermelonDB

1.  **Install WatermelonDB**:
    ```bash
    npm install @nozbe/watermelondb @nozbe/watermelondb-sqlite
    cd android && ./gradlew installJSI
    ```
2.  **Define your Database Schema (`database/schema.ts`)**:
    Create a schema that defines your tables and columns. Ensure you have a `syncStatus` column (or similar) in tables that need to be synced.
    ```typescript
    // database/schema.ts
    import { appSchema, tableSchema } from '@nozbe/watermelondb';

    export const schema = appSchema({
      version: 1, // Increment this version when you make schema changes
      tables: [
        tableSchema({
          name: 'tasks',
          columns: [
            { name: 'name', type: 'string' },
            { name: 'is_completed', type: 'boolean' },
            { name: 'sync_status', type: 'string', isIndexed: true }, // 'pending', 'synced'
            { name: 'created_at', type: 'number' },
            { name: 'updated_at', type: 'number' },
          ],
        }),
        // Add other table schemas here
      ],
    });
    ```
3.  **Define your Models (`database/models/Task.ts`)**:
    Create models for each table, extending `Model` from WatermelonDB.
    ```typescript
    // database/models/Task.ts
    import { Model } from '@nozbe/watermelondb';
    import { field, date, text } from '@nozbe/watermelondb/decorators';

    export class Task extends Model {
      static table = 'tasks';

      @text('name') name!: string;
      @field('is_completed') isCompleted!: boolean;
      @field('sync_status') syncStatus!: 'pending' | 'synced';
      @date('created_at') createdAt!: number;
      @date('updated_at') updatedAt!: number;
    }
    ```
4.  **Initialize the Database (`database/database.ts`)**:
    ```typescript
    // database/database.ts
    import { Database } from '@nozbe/watermelondb';
    import SQLiteAdapter from '@nozbe/watermelondb/adapters/sqlite';
    import { schema } from './schema';
    import { Task } from './models/Task'; // Import all your models

    const adapter = new SQLiteAdapter({
      schema,
      // jsi: false, // Use JSI for better performance (default in newer versions)
    });

    export const database = new Database({
      adapter,
      modelClasses: [Task], // List all your models here
    });
    ```

### Step 2: Implement Sync Logic (`services/syncService.ts`)

1.  **Install NetInfo**:
    ```bash
    npm install @react-native-community/netinfo
    cd ios && pod install # If you also target iOS
    ```
2.  **Create `services/syncService.ts`**:
    Implement `hasPendingChanges`, `syncPendingChanges`, `fetchServerUpdates`, and `fullSync` functions. Adapt the API calls (`FAKE_API_URL`) to your actual backend.
    ```typescript
    // services/syncService.ts
    import NetInfo from '@react-native-community/netinfo';
    import { database } from '../database/database';
    import { Task } from '../database/models/Task'; // Import your models

    const YOUR_EAM_API_URL = 'https://your-backend.com/api/tasks'; // Replace with your actual API

    export async function hasPendingChanges(): Promise<boolean> {
      const tasksCollection = database.get<Task>('tasks');
      const pendingTasks = await tasksCollection.query().fetch();
      return pendingTasks.some(t => t.syncStatus === 'pending');
    }

    export async function syncPendingChanges(): Promise<boolean> {
      console.log('🔄 Starting upload sync...');
      const netInfo = await NetInfo.fetch();
      if (!netInfo.isConnected) {
        console.log('❌ No internet connection for upload.');
        return false;
      }

      try {
        const tasksCollection = database.get<Task>('tasks');
        const pendingTasks = await tasksCollection.query().fetch();
        const tasksToSync = pendingTasks.filter(t => t.syncStatus === 'pending');

        if (tasksToSync.length === 0) {
          console.log('✅ No pending changes to upload.');
          return true;
        }

        console.log(`📤 Uploading ${tasksToSync.length} tasks...`);

        for (const task of tasksToSync) {
          try {
            const response = await fetch(`${YOUR_EAM_API_URL}/${task.id}`, { // Adjust API endpoint
              method: 'PUT', // Or POST for new items
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                name: task.name,
                isCompleted: task.isCompleted,
                // ... other fields
              }),
            });

            if (response.ok) {
              await database.write(async () => {
                await task.update(t => {
                  t.syncStatus = 'synced';
                });
              });
              console.log(`✅ Uploaded task: ${task.name}`);
            } else {
              console.error(`❌ Failed to upload task ${task.id}: ${response.statusText}`);
            }
          } catch (error) {
            console.error(`❌ Failed to upload task ${task.id}:`, error);
          }
        }
        console.log('✅ Upload sync completed!');
        return true;
      } catch (error) {
        console.error('❌ Upload sync failed:', error);
        return false;
      }
    }

    export async function fetchServerUpdates(): Promise<void> {
      console.log('📥 Fetching server updates...');
      const netInfo = await NetInfo.fetch();
      if (!netInfo.isConnected) {
        console.log('❌ No internet connection for download.');
        return;
      }

      try {
        const response = await fetch(YOUR_EAM_API_URL); // Adjust API endpoint
        if (response.ok) {
          const serverData = await response.json();
          await database.write(async () => {
            // Example: For each item from server, either create or update local record
            for (const serverItem of serverData) {
              let localItem = await database.get<Task>('tasks').find(serverItem.id); // Assuming serverItem.id matches local id
              if (localItem) {
                await localItem.update(item => {
                  item.name = serverItem.name;
                  item.isCompleted = serverItem.isCompleted;
                  item.syncStatus = 'synced'; // Mark as synced from server
                });
              } else {
                await database.get<Task>('tasks').create(item => {
                  item.id = serverItem.id;
                  item.name = serverItem.name;
                  item.isCompleted = serverItem.isCompleted;
                  item.syncStatus = 'synced';
                });
              }
            }
          });
          console.log('✅ Server updates fetched and applied.');
        } else {
          console.error(`❌ Failed to fetch server updates: ${response.statusText}`);
        }
      } catch (error) {
        console.error('❌ Failed to fetch server updates:', error);
      }
    }

    export async function fullSync(): Promise<void> {
      console.log('🔄 Starting full sync...');
      await syncPendingChanges();
      await fetchServerUpdates();
      console.log('✅ Full sync completed!');
    }
    ```

### Step 3: Create Headless JS Task (`headlessTask.js`)

1.  **Create `headlessTask.js`**:
    ```javascript
    // headlessTask.js
    import { AppRegistry, NativeModules } from 'react-native';
    import { fullSync, hasPendingChanges } from './services/syncService';

    const { BackgroundSyncModule } = NativeModules;

    const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    const BackgroundSync = async (taskData) => {
        console.log('--- HeadlessJS Sync Task Start ---');

        if (!BackgroundSyncModule || !BackgroundSyncModule.updateNotification) {
            console.error("BackgroundSyncModule not found. Cannot update notification.");
            return;
        }

        try {
            const pending = await hasPendingChanges();
            if (!pending) {
                console.log('--- HeadlessJS: No pending changes. Skipping sync. ---');
                await BackgroundSyncModule.resetNotification();
                return;
            }

            await BackgroundSyncModule.updateNotification("Sync in progress...", true);
            await fullSync();
            await BackgroundSyncModule.updateNotification("Sync complete! All items are up to date.", false);
            console.log('--- HeadlessJS Sync Task Success ---');
            await sleep(5000);
            await BackgroundSyncModule.resetNotification();

        } catch (error) {
            console.error('--- HeadlessJS Sync Task Error ---', error);
            await BackgroundSyncModule.updateNotification("Sync failed. Please check your connection.", false);
            await sleep(5000);
            await BackgroundSyncModule.resetNotification();
        }
    };

    AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);
    ```
2.  **Import in `index.js`**:
    Ensure your main `index.js` (or `App.js` if not using Expo Router) imports the headless task file.
    ```javascript
    // index.js
    import "expo-router/entry"; // Or your main App component
    import './headlessTask'; // <--- ADD THIS LINE
    ```

### Step 4: Android Native Setup

#### 4.1: `AndroidManifest.xml`

Open `android/app/src/main/AndroidManifest.xml` and add the following permissions and service/receiver declarations within the `<application>` tag. Replace `com.testoffline.sync` with your actual package name.

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/> <!-- For Android 13+ notifications -->

<application ...>
    <!-- Your existing activities -->

    <!-- Headless JS Service for background sync -->
    <service
      android:name="com.yourpackagename.SyncHeadlessTaskService"
      android:exported="false" />

    <!-- This receiver ensures our PersistentSyncService is restarted after the phone reboots. -->
    <receiver
      android:name="com.yourpackagename.BootCompletedReceiver"
      android:enabled="true"
      android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
      </intent-filter>
    </receiver>

    <!-- This is our NEW 24/7 Foreground Service -->
    <service
      android:name="com.yourpackagename.PersistentSyncService"
      android:enabled="true"
      android:exported="false"
      android:foregroundServiceType="dataSync"
      />
</application>
```

#### 4.2: `build.gradle` (app level)

Open `android/app/build.gradle` and add WorkManager dependency:

```gradle
dependencies {
    // ... other dependencies
    implementation("androidx.work:work-runtime:2.8.1") // Or the latest stable version
}
```

Also, ensure your `namespace` is correctly set (e.g., `com.testoffline.sync`).

#### 4.3: Create Native Files

Create the following Java/Kotlin files under `android/app/src/main/java/com/yourpackagename/` (replace `com.yourpackagename` with your actual package name from `build.gradle`).

*   **`BackgroundSyncPackage.java`**:
    ```java
    // android/app/src/main/java/com/yourpackagename/BackgroundSyncPackage.java
    package com.yourpackagename; // Adjust package name

    import com.facebook.react.ReactPackage;
    import com.facebook.react.bridge.NativeModule;
    import com.facebook.react.bridge.ReactApplicationContext;
    import com.facebook.react.uimanager.ViewManager;

    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;

    import androidx.annotation.NonNull;

    public class BackgroundSyncPackage implements ReactPackage {
        @NonNull
        @Override
        public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
            List<NativeModule> modules = new ArrayList<>();
            modules.add(new BackgroundSyncModule(reactContext));
            return modules;
        }

        @NonNull
        @Override
        public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
            return Collections.emptyList();
        }
    }
    ```

*   **`BackgroundSyncModule.java`**:
    ```java
    // android/app/src/main/java/com/yourpackagename/BackgroundSyncModule.java
    package com.yourpackagename; // Adjust package name

    import android.app.Notification;
    import android.app.NotificationManager;
    import android.app.PendingIntent;
    import android.content.Context;
    import android.content.Intent;
    import android.net.Uri;
    import android.os.Build;
    import android.os.PowerManager;
    import android.provider.Settings;
    import android.util.Log;

    import com.facebook.react.bridge.ReactApplicationContext;
    import com.facebook.react.bridge.ReactContextBaseJavaModule;
    import com.facebook.react.bridge.ReactMethod;
    import com.facebook.react.bridge.Promise;
    import com.facebook.react.modules.core.DeviceEventManagerModule;

    import androidx.annotation.NonNull;
    import androidx.core.app.NotificationCompat;

    // Make sure these imports are correct for your project
    import com.yourpackagename.MainActivity; // Adjust if your MainActivity is in a different package
    import com.yourpackagename.R; // Adjust if your R file is in a different package

    public class BackgroundSyncModule extends ReactContextBaseJavaModule {
        private static final String TAG = "BackgroundSyncModule";
        private static ReactApplicationContext reactContext;

        public BackgroundSyncModule(ReactApplicationContext context) {
            super(context);
            reactContext = context;
        }

        @NonNull
        @Override
        public String getName() {
            return "BackgroundSyncModule";
        }

        @ReactMethod
        public void requestIgnoreBatteryOptimizations(Promise promise) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Context context = getReactApplicationContext();
                String packageName = context.getPackageName();
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                    Log.d(TAG, "Requesting battery optimization exemption.");
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        promise.resolve("requested");
                    } catch (Exception e) {
                        Log.e(TAG, "Error requesting battery optimization", e);
                        promise.reject("E_ERROR", e.getMessage());
                    }
                } else {
                    Log.d(TAG, "Battery optimization already ignored.");
                    promise.resolve("already_ignored");
                }
            } else {
                promise.resolve("not_needed");
            }
        }

        @ReactMethod
        public void updateNotification(String text, boolean ongoing, Promise promise) {
            try {
                Context context = getReactApplicationContext();
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager == null) {
                    promise.reject("E_NO_MANAGER", "NotificationManager is null");
                    return;
                }

                Intent notificationIntent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

                Notification notification = new NotificationCompat.Builder(context, PersistentSyncService.CHANNEL_ID)
                    .setContentTitle("Offline App")
                    .setContentText(text)
                    .setSmallIcon(R.mipmap.ic_launcher) // Ensure you have an ic_launcher in your mipmap folders
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(ongoing)
                    .build();

                notificationManager.notify(PersistentSyncService.NOTIFICATION_ID, notification);
                promise.resolve(true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update notification", e);
                promise.reject("E_NOTIF_UPDATE", e.getMessage());
            }
        }

        @ReactMethod
        public void resetNotification(Promise promise) {
            updateNotification("Service is running in background", true, promise);
        }

        @ReactMethod
        public void triggerSync(Promise promise) {
            try {
                Context context = getReactApplicationContext();
                SyncWorker.scheduleOneTimeSync(context);
                Log.d(TAG, "✅ Sync triggered manually");
                promise.resolve("triggered");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to trigger sync: " + e.getMessage());
                promise.reject("ERROR", e.getMessage());
            }
        }

        public static void sendNetworkChangeEvent(String status) {
            if (reactContext != null) {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("NetworkStatusChanged", status);
            }
        }
    }
    ```

*   **`BootCompletedReceiver.java`**:
    ```java
    // android/app/src/main/java/com/yourpackagename/BootCompletedReceiver.java
    package com.yourpackagename; // Adjust package name

    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.os.Build;
    import android.util.Log;

    public class BootCompletedReceiver extends BroadcastReceiver {

        private static final String TAG = "BootCompletedReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Log.i(TAG, "Phone rebooted. Restarting services.");

                // 1. Re-schedule the 15-minute backup worker
                SyncWorker.schedulePeriodicSync(context);

                // 2. Restart the Persistent "Watchman" Service
                Intent serviceIntent = new Intent(context, PersistentSyncService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
    ```

*   **`PersistentSyncService.java`**:
    ```java
    // android/app/src/main/java/com/yourpackagename/PersistentSyncService.java
    package com.yourpackagename; // Adjust package name

    import android.app.Notification;
    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.app.PendingIntent;
    import android.app.Service;
    import android.content.Context;
    import android.content.Intent;
    import android.net.ConnectivityManager;
    import android.net.Network;
    import android.net.NetworkRequest;
    import android.os.Build;
    import android.os.IBinder;
    import android.util.Log;

    import androidx.annotation.NonNull;
    import androidx.core.app.NotificationCompat;

    public class PersistentSyncService extends Service {

        private static final String TAG = "PersistentSyncService";
        public static final int NOTIFICATION_ID = 1;
        public static final String CHANNEL_ID = "PersistentSyncChannel";

        private ConnectivityManager connectivityManager;
        private ConnectivityManager.NetworkCallback networkCallback;

        @Override
        public void onCreate() {
            super.onCreate();
            Log.d(TAG, "Watchman Service onCreate");
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            createNotificationChannel();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Log.i(TAG, "NETWORK AVAILABLE! Triggering instant sync.");
                    BackgroundSyncModule.sendNetworkChangeEvent("online");
                    SyncWorker.scheduleOneTimeSync(getApplicationContext());
                }

                @Override
                public void void onLost(@NonNull Network network) {
                    super.onLost(network);
                    Log.i(TAG, "NETWORK LOST.");
                    BackgroundSyncModule.sendNetworkChangeEvent("offline");
                }
            };
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "Watchman Service onStartCommand - Starting Foreground Service.");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Offline App")
                    .setContentText("Sync service is running in background")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();

            startForeground(NOTIFICATION_ID, notification);

            NetworkRequest networkRequest = new NetworkRequest.Builder().build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(TAG, "Watchman Service onDestroy - Unregistering network callback.");
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Persistent Sync Service",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(serviceChannel);
                }
            }
        }
    }
    ```

*   **`SyncHeadlessTaskService.java`**:
    ```java
    // android/app/src/main/java/com/yourpackagename/SyncHeadlessTaskService.java
    package com.yourpackagename; // Adjust package name

    import android.content.Intent;
    import android.os.Bundle;
    import com.facebook.react.HeadlessJsTaskService;
    import com.facebook.react.bridge.Arguments;
    import com.facebook.react.jstasks.HeadlessJsTaskConfig;
    import javax.annotation.Nullable;

    public class SyncHeadlessTaskService extends HeadlessJsTaskService {

        @Override
        protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
            Bundle extras = intent.getExtras();
            return new HeadlessJsTaskConfig(
                "BackgroundSync", // Name of the JS task (must match AppRegistry.registerHeadlessTask)
                extras != null ? Arguments.fromBundle(extras) : Arguments.createMap(),
                5000, // Timeout for the task in milliseconds
                true // Whether or not the task is allowed to run in foreground
            );
        }
    }
    ```

*   **`SyncWorker.java`**:
    ```java
    // android/app/src/main/java/com/yourpackagename/SyncWorker.java
    package com.yourpackagename; // Adjust package name

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
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            );

            Log.d("SyncWorker", "Periodic sync work (15 min) scheduled with WorkManager.");
        }

        public static void scheduleOneTimeSync(Context context) {
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

            OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_SYNC_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            );
            Log.d("SyncWorker", "One-time immediate sync work scheduled with WorkManager.");
        }
    }
    ```

#### 4.4: Register Native Package in `MainApplication.kt`

Open `android/app/src/main/java/com/yourpackagename/MainApplication.kt` and make the following changes:

1.  **Add Imports**:
    ```kotlin
    import android.content.Intent
    import android.os.Build
    import com.yourpackagename.SyncWorker // Adjust package name
    import com.yourpackagename.PersistentSyncService // Adjust package name
    ```
2.  **Add `BackgroundSyncPackage()` to `getPackages()`**:
    ```kotlin
    override fun getPackages(): List<ReactPackage> {
        val packages = PackageList(this).packages.toMutableList()
        // Add our custom background sync package
        packages.add(BackgroundSyncPackage()) // <--- ADD THIS LINE
        return packages
    }
    ```
3.  **Schedule Sync and Start Service in `onCreate()`**:
    ```kotlin
    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)
        // ... existing code

        // Schedule the 15-minute reliable backup sync.
        SyncWorker.schedulePeriodicSync(applicationContext)

        // Start the Persistent "Watchman" Service
        val serviceIntent = Intent(this, PersistentSyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    ```

### Step 5: Request Battery Optimization Exemption (Optional but Recommended)

For reliable background execution, especially on Android 6.0 (Marshmallow) and above, it's highly recommended to ask the user to exempt your app from battery optimizations. You can call the `requestIgnoreBatteryOptimizations` method from your JavaScript code:

```javascript
// In your React Native component or a utility function
import { NativeModules } from 'react-native';
const { BackgroundSyncModule } = NativeModules;

const requestBatteryOptimization = async () => {
  try {
    const status = await BackgroundSyncModule.requestIgnoreBatteryOptimizations();
    console.log('Battery optimization status:', status);
    // 'requested': user was prompted
    // 'already_ignored': already ignored
    // 'not_needed': Android version < M
  } catch (error) {
    console.error('Failed to request battery optimization:', error);
  }
};

// Call this function at an appropriate time, e.g., after user onboarding or in settings.
// You should explain to the user why this permission is needed.
```

### Step 6: Handle Network Status Changes in JavaScript (Optional)

If you want your JavaScript UI to react to network changes detected by the native `PersistentSyncService`, you can listen for the `NetworkStatusChanged` event:

```javascript
// In your React Native component
import { NativeEventEmitter, NativeModules } from 'react-native';
import { useEffect } from 'react';

const { BackgroundSyncModule } = NativeModules;
const eventEmitter = new NativeEventEmitter(BackgroundSyncModule);

const MyComponent = () => {
  useEffect(() => {
    const subscription = eventEmitter.addListener('NetworkStatusChanged', (status) => {
      console.log('Network status changed:', status); // 'online' or 'offline'
      // Update UI or trigger actions based on network status
    });

    return () => {
      subscription.remove();
    };
  }, []);

  // ... your component UI
};
```

## Example Scenarios

Here are some scenarios demonstrating how this background sync setup works:

*   **User adds a task offline**:
    1.  User creates a new task in the app while offline.
    2.  The task is saved to WatermelonDB with `syncStatus: 'pending'`.
    3.  The `PersistentSyncService` detects no network.
    4.  When the user goes online, `PersistentSyncService` detects network availability.
    5.  `PersistentSyncService` triggers `SyncWorker.scheduleOneTimeSync()`.
    6.  `SyncWorker` starts `SyncHeadlessTaskService`, which runs `headlessTask.js`.
    7.  `headlessTask.js` calls `fullSync()` in `syncService.ts`.
    8.  `syncService.ts` uploads the pending task to the server and updates its `syncStatus` to `'synced'` in WatermelonDB.
    9.  The notification updates to "Sync in progress..." then "Sync complete!".

*   **App is killed by the OS**:
    1.  The user closes the app or Android kills it due to memory pressure.
    2.  The `PersistentSyncService` (as a foreground service) continues to run, monitoring the network.
    3.  The periodic sync scheduled by WorkManager (`SyncWorker`) will still execute at its scheduled intervals (e.g., every 15 minutes), even if the app is killed.
    4.  If the periodic sync runs, it will trigger `headlessTask.js` to perform a sync.

*   **Device reboots**:
    1.  The device is restarted.
    2.  `BootCompletedReceiver.java` detects the `BOOT_COMPLETED` action.
    3.  It restarts `PersistentSyncService` and re-schedules the periodic `SyncWorker` task.
    4.  Background sync functionality resumes automatically.

*   **User manually triggers sync (e.g., from settings)**:
    1.  The user taps a "Sync Now" button in the app's settings.
    2.  The JavaScript code calls `NativeModules.BackgroundSyncModule.triggerSync()`.
    3.  This native method calls `SyncWorker.scheduleOneTimeSync()`.
    4.  WorkManager executes `SyncWorker`, which runs `headlessTask.js` to perform an immediate sync.

This comprehensive setup provides a robust and reliable background synchronization mechanism for your React Native Android application, ensuring data consistency even in challenging network conditions or when the app is not actively in use.

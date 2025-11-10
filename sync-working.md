# Background Sync Architecture

This document outlines the robust, 24/7 background synchronization system for this React Native application. The system is designed to be resilient, ensuring data syncs even if the app is killed, the phone is offline, or the device is restarted.

The architecture is split into three main parts:

1.  **The Native "Engine" (Android - Java/Kotlin):** Components that run 24/7 at the OS level to manage *when* to sync.
2.  **The JavaScript "Brain" (React Native - JS/TS):** The logic that defines *what* to sync.
3.  **The "Bridge" & Configuration (Glue):** The files that connect the Native Engine to the JS Brain and declare capabilities to the Android OS.

---

## Core Components: File-by-File

Here are the key files involved in this system and their primary role.

### 🤖 The Native "Engine" (Android)

These are the Java/Kotlin files that operate at the OS level, even when the app is closed.

* **`android/app/src/main/java/com/testoffline/sync/PersistentSyncService.java` (The 24/7 "Watchman" 🕵️)**
    * **What it is:** This is a "Foreground Service." It runs 24/7 in the background.
    * **Why it's here:** Android aggressively kills background tasks to save battery. A Foreground Service tells the OS, "I am doing important work, do not kill me." It must show a **persistent notification** (e.g., "Service is running...") to inform the user it's active.
    * **Key Function:** Its main job is to register a `NetworkCallback`. When it detects that the phone has regained an internet connection (`onAvailable`), it immediately triggers the `SyncWorker` to start an instant sync.

* **`android/app/src/main/java/com/testoffline/sync/BootCompletedReceiver.java` (The "Alarm Clock" ⏰)**
    * **What it is:** This is a "Broadcast Receiver." It listens for specific signals from the Android OS.
    * **Why it's here:** When the user restarts their phone, all services (including our Watchman) are stopped.
    * **Key Function:** It listens for one signal: `BOOT_COMPLETED` (phone restart). As soon as the phone finishes booting, this receiver's *only* job is to restart the `PersistentSyncService` so it can go back on duty.

* **`android/app/src/main/java/com/testoffline/sync/SyncWorker.java` (The "Reliable Worker" 👷)**
    * **What it is:** This uses Android's `WorkManager` to schedule and run the sync task reliably.
    * **Why it's here:** `WorkManager` is the modern Android standard for **guaranteed, battery-efficient** background work. It handles retries and constraints (like "only run if network is connected") automatically.
    * **Key Function:**
        * [cite_start]`schedulePeriodicSync()`: Schedules a "backup" sync to run every 15 minutes [cite: 299-300]. This is a fallback.
        * `scheduleOneTimeSync()`: Triggers an **immediate, one-time sync**. [cite_start]This is the main function called by the Watchman when the internet returns [cite: 301-302].
        * `doWork()`: When the worker is told to run, its *only* job is to start the `SyncHeadlessTaskService` (the bridge).

* **`android/app/src/main/java/com/testoffline/sync/MainApplication.kt` (The "Ignition Key" 🔑)**
    * **What it is:** The main entry point for the Android application, which runs when the user opens the app.
    * **Why it's here:** We need a place to start the background services the very first time the user opens the app.
    * **Key Function:** In its `onCreate` method, it:
        1.  Starts the `PersistentSyncService` (the Watchman) for the first time.
        2.  Schedules the 15-minute `schedulePeriodicSync` (the backup plan).

---

### 🧠 The JavaScript "Brain" (React Native)

These are the JS/TS files that define *what* logic to run when a sync is triggered.

* **`headlessTask.js` (The "Background JS Entry Point")**
    * **What it is:** This is a special JavaScript file that can be run in the background, without any UI.
    * **Why it's here:** The Native "Engine" (Java) cannot directly run your app's UI components (`app/index.tsx`). It needs a dedicated, lightweight JS entry point to call.
    * **Key Function:**
        1.  [cite_start]Registers itself with React Native using `AppRegistry.registerHeadlessTask('BackgroundSync', ...)`[cite: 432].
        2.  [cite_start]Calls the `BackgroundSyncModule` (the bridge) to update the notification (e.g., "Sync in progress...")[cite: 430].
        3.  [cite_start]Calls the *actual* sync logic from `services/syncService.ts` (e.g., `fullSync()`)[cite: 430].
        4.  [cite_start]Updates the notification again on success ("Sync complete!") or failure ("Sync failed.") [cite: 431-432].

* **`services/syncService.ts` (The "Sync Logic")**
    * **What it is:** This file contains all the business logic for synchronization.
    * **Why it's here:** To keep the "what to do" (sync logic) separate from the "when to do it" (native engine).
    * **Key Function:**
        1.  [cite_start]`hasPendingChanges()`: Checks WatermelonDB for any records with `syncStatus: 'pending'` [cite: 442-444].
        2.  [cite_start]`syncPendingChanges()`: Gets all pending tasks, sends them to the server API via `fetch`, and on success, updates their local status to `syncStatus: 'synced'` in the database [cite: 445-450].
        3.  `fullSync()`: A wrapper that first *sends* local changes and then *fetches* server updates.

* **`index.js` (The "Task Registrar")**
    * **What it is:** The main JS entry point for your entire React Native app.
    * **Why it's here:** The headless task must be registered as early as possible.
    * **Key Function:** `import './headlessTask';` ensures that the `BackgroundSync` task is registered with `AppRegistry` as soon as the app starts, making it available for the native side to call.

---

### 🔗 The "Bridge" & Configuration (Glue)

These files connect the Native and JS worlds and declare permissions to the Android OS.

* **`android/app/src/main/java/com/testoffline/sync/SyncHeadlessTaskService.java` (The "Native-to-JS Bridge" 🌉)**
    * **What it is:** An Android Service whose only job is to bridge the gap between the `SyncWorker` (Java) and the `headlessTask.js` (JS).
    * **Why it's here:** `WorkManager` (`SyncWorker`) is pure Java/Kotlin. It cannot directly run a JS task. It needs to start this "bridge" service, which *can*.
    * [cite_start]**Key Function:** When started by the `SyncWorker`, it tells React Native to run the JavaScript task named `"BackgroundSync"`[cite: 296, 298].

* **`android/app/src/main/java/com/testoffline/sync/BackgroundSyncModule.java` (The "JS-to-Native Bridge / Control Panel" 🎛️)**
    * **What it is:** A custom "Native Module" that exposes Java functions to your JavaScript code.
    * **Why it's here:** Your JS code (like `headlessTask.js` and `app/index.tsx`) needs a way to control native Android features from JavaScript.
    * **Key Function:** Exposes `@ReactMethod` functions to JS:
        * [cite_start]`updateNotification()`: Allows `headlessTask.js` to change the text of the persistent notification [cite: 281-283].
        * [cite_start]`resetNotification()`: Resets the notification to its default state[cite: 284].
        * [cite_start]`requestIgnoreBatteryOptimizations()`: Allows `app/index.tsx` to open the phone's battery settings for the user [cite: 278-280].

* **`android/app/src/main/java/com/testoffline/sync/BackgroundSyncPackage.java` (The "Module Registrar")**
    * **What it is:** A simple "registrar" file.
    * [cite_start]**Why it's here:** It tells React Native's `MainApplication.kt` that the `BackgroundSyncModule` (the Control Panel) exists and should be loaded as part of the app [cite: 286-287].

* **`android/app/src/main/AndroidManifest.xml` (The "Permission Slip" 📜)**
    * **What it is:** The central configuration file for the Android app.
    * **Why it's here:** You *must* declare all special permissions and components here, or the Android OS will block them for security reasons.
    * **Key Function:**
        * [cite_start]**Permissions:** Declares `<uses-permission ... />` for `RECEIVE_BOOT_COMPLETED` (restart), `FOREGROUND_SERVICE` (24/7 service), `POST_NOTIFICATIONS` (show notifications), etc.[cite: 272].
        * **Component Registration:** Declares all our custom components to the OS so it knows they exist:
            * [cite_start]`<service android:name=".PersistentSyncService" ... />` [cite: 276]
            * [cite_start]`<receiver android:name=".BootCompletedReceiver" ... />` [cite: 275]
            * [cite_start]`<service android:name=".SyncHeadlessTaskService" ... />` [cite: 275]

---

## System Workflows (Diagrams)

This is how the files work together in different scenarios.

### Workflow 1: 🏁 App Launch (First-Time Setup)

*This flow runs when a user opens the app from their home screen.*

> ```
> [User Taps App Icon]
>     │
>     ▼
> [MainApplication.kt] (onCreate)
>     │
>     ├──► 1. Calls SyncWorker.schedulePeriodicSync()
>     │      └► [SyncWorker.java]
>     │           └► Schedules a 15-minute "backup" sync with Android WorkManager.
>     │
>     └──► 2. Calls startForegroundService() for PersistentSyncService.
>            │
>            ▼
>          [PersistentSyncService.java] (The Watchman 🕵️)
>            │
>            ├──► 1. Creates the "Service is running" persistent notification.
>            └──► 2. Starts listening for internet connection changes.
> ```
> *Simultaneously, in the UI...*
> ```
> [app/index.tsx] (UI Loads)
>     │
>     └──► (Permission Request) ──► [BackgroundSyncModule.java]
>                                    (Asks user to disable Battery Optimization).
> ```

### Workflow 2: 🔄 Phone Restart (Persistence)

*This flow ensures the system survives a device reboot.*

> ```
> [Phone Finishes Booting]
>     │
>     ▼
> [Android OS] (Broadcasts `ACTION_BOOT_COMPLETED` signal)
>     │
>     ▼
> [BootCompletedReceiver.java] (Catches the broadcast)
>     │
>     └──► Calls startForegroundService() for PersistentSyncService.
>            │
>            ▼
>          [PersistentSyncService.java] (The Watchman 🕵️)
>            │
>            └──► Watchman is back on duty: shows notification, listens for internet.
> ```

### Workflow 3: ⚡ The Sync Trigger (Internet Returns)

*This is the main workflow. It runs anytime the phone gets internet, **even if the app is killed**.*

> ```
> [Phone connects to WiFi/Data]
>     │
>     ▼
> [PersistentSyncService.java] (Watchman 🕵️ detects the internet)
> (Its `onAvailable` network callback is triggered)
>     │
>     └──► Calls SyncWorker.scheduleOneTimeSync().
>            │
>            ▼
>          [SyncWorker.java] (WorkManager runs its `doWork()` method)
>            │
>            └──► Starts the bridge service: SyncHeadlessTaskService.
>                   │
>                   ▼
>                 [SyncHeadlessTaskService.java] (The "Bridge" 🌉)
>                   │
>                   └──► Wakes up the background JS engine and triggers the "BackgroundSync" task.
>                          │
>                          ▼
>                        [headlessTask.js] (The "Background Brain" 🧠)
>                          │
>                          ├──► 1. Calls BackgroundSyncModule.updateNotification("Sync in progress...").
>                          │
>                          ├──► 2. Calls services/syncService.ts -> fullSync().
>                          │      │
>                          [cite_start]│      └► (JS code talks to WatermelonDB, fetches from API, updates DB) [cite: 442-450]
>                          │
>                          └──► 3. Calls BackgroundSyncModule.updateNotification("Sync complete!").
> ```

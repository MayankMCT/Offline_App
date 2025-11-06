import * as BackgroundFetch from 'expo-background-fetch';
import * as TaskManager from 'expo-task-manager';
import { fullSync } from './syncService';

// Name of the background task
const BACKGROUND_SYNC_TASK = 'background-sync-task';

/**
 * Define what happens when background task runs
 */
TaskManager.defineTask(BACKGROUND_SYNC_TASK, async () => {
    console.log('🕐 Background sync task triggered!');

    try {
        // Perform sync
        await fullSync();

        // Return success
        return BackgroundFetch.BackgroundFetchResult.NewData;
    } catch (error) {
        console.error('❌ Background sync failed:', error);
        return BackgroundFetch.BackgroundFetchResult.Failed;
    }
});

/**
 * Register the background sync task
 * Call this once when app starts
 */
export async function registerBackgroundSync() {
    try {
        // Check if already registered
        const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_SYNC_TASK);

        if (isRegistered) {
            console.log('✅ Background sync already registered');
            return;
        }

        // Register the task to run every 5 seconds
        await BackgroundFetch.registerTaskAsync(BACKGROUND_SYNC_TASK, {
            minimumInterval: 5, // 5 seconds
            stopOnTerminate: false, // Continue after app is closed
            startOnBoot: true, // Start after device reboot
        });

        console.log('✅ Background sync registered successfully!');
    } catch (error) {
        console.error('❌ Failed to register background sync:', error);
    }
}

/**
 * Unregister background sync (useful for testing)
 */
export async function unregisterBackgroundSync() {
    try {
        await BackgroundFetch.unregisterTaskAsync(BACKGROUND_SYNC_TASK);
        console.log('✅ Background sync unregistered');
    } catch (error) {
        console.error('❌ Failed to unregister background sync:', error);
    }
}

/**
 * Get status of background sync
 */
export async function getBackgroundSyncStatus() {
    try {
        const status = await BackgroundFetch.getStatusAsync();

        switch (status) {
            case BackgroundFetch.BackgroundFetchStatus.Available:
                return 'Available';
            case BackgroundFetch.BackgroundFetchStatus.Denied:
                return 'Denied (User disabled in settings)';
            case BackgroundFetch.BackgroundFetchStatus.Restricted:
                return 'Restricted (OS limitation)';
            default:
                return 'Unknown';
        }
    } catch (error) {
        console.error('❌ Failed to get status:', error);
        return 'Error';
    }
}

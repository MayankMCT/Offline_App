import { AppRegistry, NativeModules } from 'react-native';
import { fullSync, hasPendingChanges } from './services/syncService';

// Get our native module
const { BackgroundSyncModule } = NativeModules;

// This is a helper function to delay execution
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

const BackgroundSync = async (taskData) => {
    console.log('--- HeadlessJS Sync Task Start ---');

    // Check if module exists
    if (!BackgroundSyncModule || !BackgroundSyncModule.updateNotification) {
        console.error("BackgroundSyncModule not found. Cannot update notification.");
        return; // Exit if native module is not linked
    }

    try {
        // First, check if there's anything to sync
        const pending = await hasPendingChanges();
        if (!pending) {
            console.log('--- HeadlessJS: No pending changes. Skipping sync. ---');
            // If we skipped, just reset the notification to default "running" state
            await BackgroundSyncModule.updateNotification("Service is running", true);
            return;
        }

        // --- THIS IS YOUR "UPDATE NOTIFICATION" FIX ---
        // Update the *existing* notification to "Sync in progress..."
        // It remains non-swipeable (ongoing: true)
        await BackgroundSyncModule.updateNotification("Sync in progress...", false);

        // Run the actual sync logic
        await fullSync();

        // --- THIS IS YOUR "UPDATE NOTIFICATION" FIX ---
        // Update the *existing* notification to "Sync complete!"
        // Make it swipeable (ongoing: false) so the user can dismiss it
        await BackgroundSyncModule.updateNotification("Sync complete! All items are up to date.", false);

        console.log('--- HeadlessJS Sync Task Success ---');

        // After 5 seconds, reset the notification back to its default "Service is running" state
        // and make it non-swipeable again.
        await sleep(5000);
        await BackgroundSyncModule.updateNotification("Service is running", true);

    } catch (error) {
        console.error('--- HeadlessJS Sync Task Error ---', error);

        // --- THIS IS YOUR "UPDATE NOTIFICATION" FIX ---
        // Update the *existing* notification to "Sync failed."
        // Make it swipeable (ongoing: false)
        await BackgroundSyncModule.updateNotification("Sync failed. Please check your connection.", false);

        // After 5 seconds, reset the notification
        await sleep(5000);
        await BackgroundSyncModule.updateNotification("Service is running", true);
    }
};

AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);

import { AppRegistry } from 'react-native';
import { fullSync, hasPendingChanges } from './services/syncService';
import * as Notifications from 'expo-notifications';

const SYNC_NOTIFICATION_CHANNEL = 'sync-channel';

// Configure the notification channels
const setupNotifications = async () => {
  await Notifications.setNotificationChannelAsync(SYNC_NOTIFICATION_CHANNEL, {
    name: 'Sync Notifications',
    importance: Notifications.AndroidImportance.DEFAULT,
    vibrationPattern: [0, 250, 250, 250],
    lightColor: '#FF231F7C',
  });
};

const BackgroundSync = async (taskData) => {
    console.log('--- HeadlessJS Sync Task Start ---');

    try {
        await setupNotifications();

        // First, check if there's anything to sync
        const pending = await hasPendingChanges();
        if (!pending) {
            console.log('--- HeadlessJS: No pending changes. Skipping sync. ---');
            return;
        }

        // --- THIS IS YOUR REQUIREMENT #2 ---
        // Show "Sync in progress" notification
        await Notifications.presentNotificationAsync({
            title: 'Offline App',
            body: 'Sync in progress...',
            data: { type: 'sync-progress' },
            android: {
                channelId: SYNC_NOTIFICATION_CHANNEL,
                priority: Notifications.AndroidNotificationPriority.DEFAULT,
                sticky: false, // Notification will disappear when sync is done
            },
        });

        // Run the actual sync logic
        await fullSync();

        // --- THIS IS YOUR REQUIREMENT #2 ---
        // Show "Sync complete" notification
        await Notifications.presentNotificationAsync({
            title: 'Offline App',
            body: 'Sync complete! All items are up to date.',
            data: { type: 'sync-complete' },
            android: {
                channelId: SYNC_NOTIFICATION_CHANNEL,
                priority: Notifications.AndroidNotificationPriority.DEFAULT,
            },
        });

        console.log('--- HeadlessJS Sync Task Success ---');

    } catch (error) {
        console.error('--- HeadlessJS Sync Task Error ---', error);

        // Show "Sync failed" notification
        await Notifications.presentNotificationAsync({
            title: 'Offline App',
            body: 'Sync failed. Please check your connection.',
            data: { type: 'sync-fail' },
            android: {
                channelId: SYNC_NOTIFICATION_CHANNEL,
                priority: Notifications.AndroidNotificationPriority.DEFAULT,
            },
        });
    }
};

AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);

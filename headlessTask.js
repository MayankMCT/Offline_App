/**
 * Headless JS Task - Runs in background even when app is killed
 * This is registered with React Native's AppRegistry
 */

import { AppRegistry } from 'react-native';
import { fullSync } from './services/syncService';

// Define the background sync task
const BackgroundSync = async (taskData) => {
    console.log('🔄 [HEADLESS] Background sync task started!', taskData);

    try {
        // Perform the sync
        await fullSync();
        console.log('✅ [HEADLESS] Background sync completed successfully');
    } catch (error) {
        console.error('❌ [HEADLESS] Background sync failed:', error);
    }
};

// Register the task with a name that matches what Android expects
AppRegistry.registerHeadlessTask('BackgroundSync', () => BackgroundSync);

console.log('✅ Headless task "BackgroundSync" registered');

import NetInfo from '@react-native-community/netinfo';
import { database } from '../database/database';
import { Task } from '../database/models/Task';

// This simulates your EAM API
const FAKE_API_URL = 'https://jsonplaceholder.typicode.com/posts';

/**
 * NEW FUNCTION: Checks if there are any pending changes.
 */
export async function hasPendingChanges(): Promise<boolean> {
    const tasksCollection = database.get<Task>('tasks');
    const pendingTasks = await tasksCollection
        .query()
        .fetch();
    const tasksToSync = pendingTasks.filter(t => t.syncStatus === 'pending');
    return tasksToSync.length > 0;
}


/**
 * Main sync function - syncs pending changes to server
 */
export async function syncPendingChanges(): Promise<boolean> {
    console.log('🔄 Starting sync...');

    // Check if we have internet
    const netInfo = await NetInfo.fetch();
    if (!netInfo.isConnected) {
        console.log('❌ No internet connection');
        return false;
    }

    try {
        // Get all tasks with pending sync status
        const tasksCollection = database.get<Task>('tasks');
        const pendingTasks = await tasksCollection
            .query()
            .fetch();

        const tasksToSync = pendingTasks.filter(t => t.syncStatus === 'pending');

        if (tasksToSync.length === 0) {
            console.log('✅ No pending changes to sync');
            return true;
        }

        console.log(`📤 Syncing ${tasksToSync.length} tasks...`);

        // Simulate a network delay for 2 seconds so we can see the notification
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Sync each task to server
        for (const task of tasksToSync) {
            try {
                // Send to your EAM API (this is fake for demo)
                const response = await fetch(FAKE_API_URL, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        name: task.name,
                        isCompleted: task.isCompleted,
                    }),
                });

                if (response.ok) {
                    // Mark as synced in local database
                    await database.write(async () => {
                        await task.update(t => {
                            t.syncStatus = 'synced';
                        });
                    });
                    console.log(`✅ Synced task: ${task.name}`);
                }
            } catch (error) {
                console.error(`❌ Failed to sync task ${task.id}:`, error);
                // Continue with other tasks
            }
        }

        console.log('✅ Sync completed!');
        return true;
    } catch (error) {
        console.error('❌ Sync failed:', error);
        return false;
    }
}

/**
 * Fetch new data from server and update local database
 * (This would fetch updates from your EAM)
 */
export async function fetchServerUpdates(): Promise<void> {
    console.log('📥 Fetching server updates...');

    // Check if we have internet
    const netInfo = await NetInfo.fetch();
    if (!netInfo.isConnected) {
        console.log('❌ No internet connection');
        return;
    }

    try {
        // Fetch from your EAM API (this is fake for demo)
        const response = await fetch(FAKE_API_URL);

        if (response.ok) {
            // const serverData = await response.json();
            // Update local database with server data
            // (Implementation depends on your EAM API structure)
            console.log('✅ Server updates fetched');
        }
    } catch (error) {
        console.error('❌ Failed to fetch server updates:', error);
    }
}

/**
 * Full sync - send local changes AND get server updates
 */
export async function fullSync(): Promise<void> {
    console.log('🔄 Starting full sync...');

    // First, send our pending changes
    await syncPendingChanges();

    // Then, get updates from server
    await fetchServerUpdates();

    console.log('✅ Full sync completed!');
}

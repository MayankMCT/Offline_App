import NetInfo from '@react-native-community/netinfo';
import React, { useEffect, useState } from 'react';
import {
    Alert,
    FlatList,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from 'react-native';
import { database } from '../database/database';
import { Task } from '../database/models/Task';
import { syncPendingChanges } from '../services/syncService';

export default function HomeScreen() {
    const [taskName, setTaskName] = useState('');
    const [tasks, setTasks] = useState<Task[]>([]);
    const [isOnline, setIsOnline] = useState(false);
    const [syncStatus, setSyncStatus] = useState('');

    // Monitor network status and auto-sync when coming online
    useEffect(() => {
        const unsubscribe = NetInfo.addEventListener(state => {
            const wasOffline = !isOnline;
            const nowOnline = state.isConnected ?? false;

            setIsOnline(nowOnline);

            // Auto-sync when network comes back online
            if (wasOffline && nowOnline) {
                console.log('🌐 Network restored - auto-syncing...');
                syncPendingChanges()
                    .then(() => {
                        console.log('✅ Auto-sync completed');
                        loadTasks();
                    })
                    .catch(err => console.error('❌ Auto-sync failed:', err));
            }
        });

        return () => unsubscribe();
    }, [isOnline]);

    // Periodic sync every 5 seconds when online
    useEffect(() => {
        if (!isOnline) return;

        console.log('⏱️ Starting periodic sync (every 5 seconds)');

        const interval = setInterval(async () => {
            console.log('🔄 Periodic sync triggered');
            try {
                const result = await syncPendingChanges();
                if (result) {
                    loadTasks();
                }
            } catch (error) {
                console.error('❌ Periodic sync error:', error);
            }
        }, 5000); // 5 seconds

        return () => {
            console.log('⏹️ Stopping periodic sync');
            clearInterval(interval);
        };
    }, [isOnline]);

    // Load tasks from database
    useEffect(() => {
        loadTasks();
    }, []);

    const loadTasks = async () => {
        const tasksCollection = database.get<Task>('tasks');
        const allTasks = await tasksCollection.query().fetch();
        setTasks(allTasks);
    };

    // Add new task (works offline)
    const addTask = async () => {
        if (!taskName.trim()) {
            Alert.alert('Error', 'Please enter a task name');
            return;
        }

        try {
            await database.write(async () => {
                await database.get<Task>('tasks').create(task => {
                    task.name = taskName;
                    task.syncStatus = 'pending'; // Mark as pending sync
                    task.isCompleted = false;
                });
            });

            setTaskName('');
            loadTasks();
            Alert.alert('Success', 'Task saved locally! Will sync when online.');

            // Auto-sync if online
            if (isOnline) {
                console.log('📤 Auto-syncing new task...');
                syncPendingChanges()
                    .then(() => {
                        console.log('✅ Task synced automatically');
                        loadTasks();
                    })
                    .catch(err => console.error('❌ Auto-sync failed:', err));
            }
        } catch (error) {
            Alert.alert('Error', 'Failed to save task');
            console.error(error);
        }
    };

    // Toggle task completion
    const toggleTask = async (task: Task) => {
        try {
            await database.write(async () => {
                await task.update(t => {
                    t.isCompleted = !t.isCompleted;
                    t.syncStatus = 'pending'; // Mark as needing sync
                });
            });
            loadTasks();

            // Auto-sync if online
            if (isOnline) {
                console.log('📤 Auto-syncing task update...');
                syncPendingChanges()
                    .then(() => {
                        console.log('✅ Task update synced automatically');
                        loadTasks();
                    })
                    .catch(err => console.error('❌ Auto-sync failed:', err));
            }
        } catch (error) {
            console.error(error);
        }
    };

    // Delete task
    const deleteTask = async (task: Task) => {
        try {
            await database.write(async () => {
                await task.markAsDeleted(); // Soft delete
            });
            loadTasks();
        } catch (error) {
            console.error(error);
        }
    };

    // Manual sync button
    const handleManualSync = async () => {
        if (!isOnline) {
            Alert.alert('Offline', 'No internet connection. Changes will sync automatically when online.');
            return;
        }

        setSyncStatus('Syncing...');
        try {
            await syncPendingChanges();
            setSyncStatus('Sync completed!');
            loadTasks();
            setTimeout(() => setSyncStatus(''), 3000);
        } catch (error) {
            setSyncStatus('Sync failed');
            setTimeout(() => setSyncStatus(''), 3000);
        }
    };

    return (
        <View style={styles.container}>
            {/* Network Status */}
            <View style={[styles.statusBar, isOnline ? styles.online : styles.offline]}>
                <Text style={styles.statusText}>
                    {isOnline ? '🟢 ONLINE' : '🔴 OFFLINE'}
                </Text>
                {syncStatus ? <Text style={styles.statusText}>{syncStatus}</Text> : null}
            </View>

            {/* Info Box */}
            <View style={styles.infoBox}>
                <Text style={styles.infoText}>
                    ℹ️ This app syncs automatically every 5 seconds when online.
                    {'\n'}Also syncs immediately when you add/update tasks!
                    {'\n'}Try turning off WiFi and adding tasks!
                </Text>
            </View>

            {/* Add Task Input */}
            <View style={styles.inputContainer}>
                <TextInput
                    style={styles.input}
                    placeholder="Enter task name..."
                    value={taskName}
                    onChangeText={setTaskName}
                />
                <TouchableOpacity style={styles.addButton} onPress={addTask}>
                    <Text style={styles.buttonText}>Add</Text>
                </TouchableOpacity>
            </View>

            {/* Manual Sync Button */}
            <TouchableOpacity style={styles.syncButton} onPress={handleManualSync}>
                <Text style={styles.syncButtonText}>🔄 Manual Sync</Text>
            </TouchableOpacity>

            {/* Tasks List */}
            <FlatList
                data={tasks}
                keyExtractor={item => item.id}
                renderItem={({ item }) => (
                    <View style={styles.taskItem}>
                        <TouchableOpacity
                            style={styles.taskContent}
                            onPress={() => toggleTask(item)}
                        >
                            <Text style={[styles.taskText, item.isCompleted && styles.completedTask]}>
                                {item.isCompleted ? '✓ ' : '○ '}
                                {item.name}
                            </Text>
                            <Text style={styles.syncLabel}>
                                {item.syncStatus === 'pending' ? '⏳ Pending' : '✅ Synced'}
                            </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.deleteButton}
                            onPress={() => deleteTask(item)}
                        >
                            <Text style={styles.deleteText}>🗑️</Text>
                        </TouchableOpacity>
                    </View>
                )}
                ListEmptyComponent={
                    <Text style={styles.emptyText}>No tasks yet. Add one above!</Text>
                }
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f5f5f5',
    },
    statusBar: {
        padding: 12,
        alignItems: 'center',
        flexDirection: 'row',
        justifyContent: 'center',
        gap: 15,
    },
    online: {
        backgroundColor: '#4CAF50',
    },
    offline: {
        backgroundColor: '#f44336',
    },
    statusText: {
        color: 'white',
        fontWeight: 'bold',
        fontSize: 14,
    },
    infoBox: {
        backgroundColor: '#E3F2FD',
        padding: 12,
        margin: 12,
        borderRadius: 8,
        borderLeftWidth: 4,
        borderLeftColor: '#2196F3',
    },
    infoText: {
        fontSize: 12,
        color: '#1976D2',
        lineHeight: 18,
    },
    inputContainer: {
        flexDirection: 'row',
        padding: 12,
        gap: 8,
    },
    input: {
        flex: 1,
        backgroundColor: 'white',
        padding: 12,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: '#ddd',
    },
    addButton: {
        backgroundColor: '#2196F3',
        padding: 12,
        borderRadius: 8,
        justifyContent: 'center',
        minWidth: 70,
    },
    buttonText: {
        color: 'white',
        fontWeight: 'bold',
        textAlign: 'center',
    },
    syncButton: {
        backgroundColor: '#FF9800',
        padding: 14,
        marginHorizontal: 12,
        marginBottom: 12,
        borderRadius: 8,
        alignItems: 'center',
    },
    syncButtonText: {
        color: 'white',
        fontWeight: 'bold',
        fontSize: 16,
    },
    taskItem: {
        backgroundColor: 'white',
        marginHorizontal: 12,
        marginVertical: 6,
        padding: 14,
        borderRadius: 8,
        flexDirection: 'row',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
    },
    taskContent: {
        flex: 1,
    },
    taskText: {
        fontSize: 16,
        color: '#333',
    },
    completedTask: {
        textDecorationLine: 'line-through',
        color: '#999',
    },
    syncLabel: {
        fontSize: 11,
        color: '#666',
        marginTop: 4,
    },
    deleteButton: {
        padding: 8,
    },
    deleteText: {
        fontSize: 20,
    },
    emptyText: {
        textAlign: 'center',
        color: '#999',
        marginTop: 40,
        fontSize: 16,
    },
});

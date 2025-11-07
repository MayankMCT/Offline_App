// app/index.tsx

import React, { useEffect, useState } from 'react';
import {
  Alert,
  FlatList,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  PermissionsAndroid,
  Platform,
  NativeModules,
} from 'react-native';
import NetInfo from '@react-native-community/netinfo';
import { database } from '../database/database';
import { Task } from '../database/models/Task';
import { syncPendingChanges } from '../services/syncService';
import { startForegroundSync, stopForegroundSync } from './foregroundSync';

// --- DEBUG: show which methods are exposed by the native module
const { BackgroundSyncModule } = NativeModules;
console.log(
  'NativeModules.BackgroundSyncModule keys =',
  BackgroundSyncModule && Object.keys(BackgroundSyncModule)
);

// Ask for Android 13+ notification permission (for foreground/worker notifs)
const requestNotificationPermission = async () => {
  if (Platform.OS === 'android' && Platform.Version >= 33) {
    try {
      // @ts-ignore - literal string
      const permission = PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS;
      const granted = await PermissionsAndroid.request(permission, {
        title: 'Notification Permission',
        message: 'This app needs permission to show sync notifications.',
        buttonNeutral: 'Ask Me Later',
        buttonNegative: 'Cancel',
        buttonPositive: 'OK',
      });
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log('You can use notifications');
      } else {
        console.log('Notification permission denied');
      }
    } catch (err) {
      console.warn(err);
    }
  }
};

export default function HomeScreen() {
  const [taskName, setTaskName] = useState('');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isOnline, setIsOnline] = useState(false);
  const [syncStatus, setSyncStatus] = useState('');

  // On mount: permissions + start persistent foreground listener (native)
  useEffect(() => {
    (async () => {
      await requestNotificationPermission();

      // Optional but recommended on aggressive OEMs
      try {
        await BackgroundSyncModule?.requestIgnoreBatteryOptimizations?.();
      } catch {}

      // Start the always-on network listener foreground service (native)
      if (Platform.OS === 'android') {
        try {
          await BackgroundSyncModule?.startPersistentService?.();
          console.log(
            'App started. Native NetworkChangeReceiver is active and will trigger sync when network connects (even if app is killed).'
          );
        } catch (e) {
          console.warn('startPersistentService error:', e);
        }
      }
    })();
  }, []);

  // Monitor network status while app is OPEN and auto-sync on restore
  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener(state => {
      const wasOffline = !isOnline;
      const nowOnline = !!state.isConnected;

      setIsOnline(nowOnline);

      if (wasOffline && nowOnline) {
        console.log('🌐 Network restored (app open) - auto-syncing...');
        syncPendingChanges()
          .then(() => {
            console.log('✅ Auto-sync completed (app open)');
            loadTasks();
          })
          .catch(err => console.error('❌ Auto-sync failed (app open):', err));
      }
    });

    return () => unsubscribe();
  }, [isOnline]);

  // Initial load
  useEffect(() => {
    loadTasks();
  }, []);

  const loadTasks = async () => {
    const tasksCollection = database.get<Task>('tasks');
    const allTasks = await tasksCollection.query().fetch();
    setTasks(allTasks);
  };

  // Create task (offline-first)
  const addTask = async () => {
    if (!taskName.trim()) {
      Alert.alert('Error', 'Please enter a task name');
      return;
    }
    try {
      await database.write(async () => {
        await database.get<Task>('tasks').create(task => {
          task.name = taskName;
          task.syncStatus = 'pending';
          task.isCompleted = false;
        });
      });

      setTaskName('');
      loadTasks();

      if (isOnline) {
        console.log('📤 Auto-syncing new task...');
        await syncPendingChanges();
        console.log('✅ Task synced automatically');
        loadTasks();
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to save task');
      console.error(error);
    }
  };

  // Toggle complete
  const toggleTask = async (task: Task) => {
    try {
      await database.write(async () => {
        await task.update(t => {
          t.isCompleted = !t.isCompleted;
          t.syncStatus = 'pending';
        });
      });
      loadTasks();

      if (isOnline) {
        console.log('📤 Auto-syncing task update...');
        await syncPendingChanges();
        console.log('✅ Task update synced automatically');
        loadTasks();
      }
    } catch (error) {
      console.error(error);
    }
  };

  // Delete (soft delete)
  const deleteTask = async (task: Task) => {
    try {
      await database.write(async () => {
        await task.markAsDeleted();
      });
      loadTasks();
    } catch (error) {
      console.error(error);
    }
  };

  // Manual sync
  const handleManualSync = async () => {
    if (!isOnline) {
      Alert.alert(
        'Offline',
        'No internet connection. Changes will sync automatically when online.'
      );
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
        <Text style={styles.statusText}>{isOnline ? '🟢 ONLINE' : '🔴 OFFLINE'}</Text>
        {syncStatus ? <Text style={styles.statusText}>{syncStatus}</Text> : null}
      </View>

      {/* Info Box */}
      <View style={styles.infoBox}>
        <Text style={styles.infoText}>
          ℹ️ This app now runs a Foreground Service.
          {'\n'}A persistent notification shows it's always listening.
          {'\n'}When you connect to the internet (even if app is killed), sync will start
          instantly.
        </Text>
      </View>

      {/* Add Task */}
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

      {/* Manual Sync */}
      <TouchableOpacity style={styles.syncButton} onPress={handleManualSync}>
        <Text style={styles.syncButtonText}>🔄 Manual Sync</Text>
      </TouchableOpacity>

      {/* Foreground Service controls (optional) */}
      <TouchableOpacity style={styles.button} onPress={() => startForegroundSync()}>
        <Text style={styles.buttonText}>Start Foreground Sync</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, { backgroundColor: '#444' }]}
        onPress={() => stopForegroundSync()}
      >
        <Text style={styles.buttonText}>Stop Foreground Sync</Text>
      </TouchableOpacity>

      {/* Tasks List */}
      <FlatList
        data={tasks}
        keyExtractor={item => item.id}
        renderItem={({ item }) => (
          <View style={styles.taskItem}>
            <TouchableOpacity style={styles.taskContent} onPress={() => toggleTask(item)}>
              <Text style={[styles.taskText, item.isCompleted && styles.completedTask]}>
                {item.isCompleted ? '✓ ' : '○ '}
                {item.name}
              </Text>
              <Text style={styles.syncLabel}>
                {item.syncStatus === 'pending' ? '⏳ Pending' : '✅ Synced'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.deleteButton} onPress={() => deleteTask(item)}>
              <Text style={styles.deleteText}>🗑️</Text>
            </TouchableOpacity>
          </View>
        )}
        ListEmptyComponent={<Text style={styles.emptyText}>No tasks yet. Add one above!</Text>}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5ff' },
  statusBar: {
    paddingTop: 40,
    paddingBottom: 12,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 15,
  },
  online: { backgroundColor: '#4CAF50' },
  offline: { backgroundColor: '#f44336' },
  statusText: { color: 'white', fontWeight: 'bold', fontSize: 14 },
  infoBox: {
    backgroundColor: '#E3F2FD',
    padding: 12,
    margin: 12,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#2196F3',
  },
  infoText: { fontSize: 13, color: '#1976D2', lineHeight: 18 },
  inputContainer: { flexDirection: 'row', padding: 12, gap: 8 },
  input: {
    flex: 1,
    backgroundColor: 'white',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    fontSize: 16,
  },
  addButton: {
    backgroundColor: '#2196F3',
    padding: 12,
    borderRadius: 8,
    justifyContent: 'center',
    minWidth: 70,
    elevation: 2,
  },
  buttonText: { color: 'white', fontWeight: 'bold', textAlign: 'center', fontSize: 16 },
  syncButton: {
    backgroundColor: '#FF9800',
    padding: 14,
    marginHorizontal: 12,
    marginBottom: 12,
    borderRadius: 8,
    alignItems: 'center',
    elevation: 2,
  },
  syncButtonText: { color: 'white', fontWeight: 'bold', fontSize: 16 },
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
  taskContent: { flex: 1 },
  taskText: { fontSize: 16, color: '#333' },
  completedTask: { textDecorationLine: 'line-through', color: '#999' },
  syncLabel: { fontSize: 11, color: '#666', marginTop: 4 },
  deleteButton: { padding: 8 },
  deleteText: { fontSize: 20 },
  emptyText: { textAlign: 'center', color: '#999', marginTop: 40, fontSize: 16 },
});

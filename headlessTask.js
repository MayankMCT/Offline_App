import { AppRegistry } from 'react-native';
import { fullSync } from './services/syncService';

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

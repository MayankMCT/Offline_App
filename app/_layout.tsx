import { Stack } from 'expo-router';
import { useEffect } from 'react';
import { registerBackgroundSync } from '../services/backgroundSync';

export default function RootLayout() {
    useEffect(() => {
        // Register background sync when app starts
        registerBackgroundSync().catch(console.error);
    }, []);

    return (
        <Stack
            screenOptions={{
                headerStyle: {
                    backgroundColor: '#f4511e',
                },
                headerTintColor: '#fff',
                headerTitleStyle: {
                    fontWeight: 'bold',
                },
            }}
        >
            <Stack.Screen
                name="index"
                options={{ title: 'Offline Sync Test' }}
            />
        </Stack>
    );
}

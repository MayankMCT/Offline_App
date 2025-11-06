// import { Stack } from 'expo-router';
// import { useEffect } from 'react';
// import { registerBackgroundSync } from '../services/backgroundSync';

// export default function RootLayout() {
//     useEffect(() => {
//         // Register background sync when app starts
//         registerBackgroundSync().catch(console.error);
//     }, []);

//     return (
//         <Stack
//             screenOptions={{
//                 headerStyle: {
//                     backgroundColor: '#f4511e',
//                 },
//                 headerTintColor: '#fff',
//                 headerTitleStyle: {
//                     fontWeight: 'bold',
//                 },
//             }}
//         >
//             <Stack.Screen
//                 name="index"
//                 options={{ title: 'Offline Sync Test' }}
//             />
//         </Stack>
//     );
// }







import { Stack } from 'expo-router';
import React, { useEffect } from 'react';
import { Platform } from 'react-native';

// NOTE: We no longer import registerBackgroundSync from '../services/backgroundSync'
// The native Android code (NetworkChangeReceiver) handles this automatically.
// The headlessTask.js is imported at the root (index.js) to register the JS-side task.

export default function RootLayout() {
    useEffect(() => {
        // Any app-wide initialization can go here.
        // We don't need to register the background sync task here anymore
        // because the native Android BroadcastReceiver handles it.
        if (Platform.OS === 'android') {
            console.log(
                'App started. Native NetworkChangeReceiver is active and will trigger sync when network connects (even if app is killed).',
            );
        }
    }, []);

    return (
        <Stack>
            <Stack.Screen
                name="index"
                options={{
                    title: 'Offline Sync App',
                    headerStyle: {
                        backgroundColor: '#f5f5f5',
                    },
                    headerShadowVisible: false,
                    headerLargeTitle: true,
                }}
            />
        </Stack>
    );
}

import { NativeModules, Platform, PermissionsAndroid } from 'react-native';

const { ForegroundSyncModule } = NativeModules;

export async function ensureNotifPermissionAndroid13Plus() {
  if (Platform.OS !== 'android') return;
  // Android 13+ Notifications permission
  try {
    await PermissionsAndroid.request?.(
      // @ts-ignore
      'android.permission.POST_NOTIFICATIONS'
    );
  } catch {}
}

export async function startForegroundSync() {
  await ensureNotifPermissionAndroid13Plus();
  ForegroundSyncModule?.start?.();
}

export function stopForegroundSync() {
  ForegroundSyncModule?.stop?.();
}

declare module 'react-native' {
  interface NativeModulesStatic {
    ForegroundSyncModule?: {
      start: () => Promise<boolean>;
      stop: () => Promise<boolean>;
    };
  }
}
